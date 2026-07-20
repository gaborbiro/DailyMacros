package dev.gaborbiro.dailymacros.features.settings.export.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import dev.gaborbiro.dailymacros.features.shared.model.TravelDay
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PdfExportOptions
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PdfPhotoMode
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** A TOC entry / bookmark target the navigation decorator turns into a link + outline item. */
data class PdfDayNav(
    val label: String,
    /** Page the clickable TOC line lives on (0-based). */
    val tocPageIndex: Int,
    /** Rectangle of the TOC line in top-left canvas coordinates. */
    val tocRect: RectF,
    /** Page the day's content starts on (0-based). */
    val targetPageIndex: Int,
)

data class RenderedDiary(
    val bytes: ByteArray,
    val pageHeightPt: Float,
    val days: List<PdfDayNav>,
)

/** The photo that best represents the meal: the flagged representative, else the first one. */
fun representativeImageFilename(template: Template): String? {
    if (template.imageFilenames.isEmpty()) return null
    val repIndex = template.isRepresentativeOfMealByImageIndex.indexOfFirst { it == true }
    return if (repIndex >= 0) template.imageFilenames[repIndex] else template.imageFilenames.first()
}

/**
 * Renders the food diary to a PDF using Android's [PdfDocument]/[Canvas] so the OS font stack draws
 * every language and colour emoji without embedded fonts. Navigation (clickable TOC + bookmarks) is
 * added afterwards by [PdfNavigationDecorator], which needs the [PdfDayNav] metadata returned here.
 *
 * [photos] maps image filename to an already-downscaled bitmap; the renderer never touches disk.
 * Meals with a narrow photo use a two-column layout (photo left, text right) to fill the page width.
 */
class PdfDiaryRenderer(
    private val title: String,
    private val rangeLabel: String,
    private val days: List<TravelDay>,
    private val options: PdfExportOptions,
    private val photos: Map<String, Bitmap>,
    private val locale: Locale = Locale.getDefault(),
) {

    private data class SizedPhoto(val bitmap: Bitmap, val width: Float, val height: Float)

    /**
     * A run of text. When [items] is set the block wraps only between items (each item stays whole,
     * separated by [MACRO_SEPARATOR]) — used for the macro line so "Fibre 3 g" never splits across a
     * line break. Otherwise [text] wraps normally at spaces.
     */
    private data class TextBlock(
        val text: String,
        val paint: Paint,
        val items: List<String>? = null,
        /** Extra vertical space before this block when stacked after another. */
        val gapBefore: Float = BLOCK_GAP,
    )

    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)

    private val doc = PdfDocument()
    private var page: PdfDocument.Page? = null
    private var canvas: Canvas = Canvas()
    private var pageIndex = -1
    private var y = MARGIN

    private val titlePaint = paint(28f, bold = true)
    private val subtitlePaint = paint(14f, color = GRAY)
    private val dayHeaderPaint = paint(23f, bold = true)
    private val totalsLabelPaint = paint(16f, bold = true)
    private val totalsValuePaint = paint(13f, color = NEAR_BLACK)
    private val mealTitlePaint = paint(16f, bold = true)
    private val bodyPaint = paint(13f)
    private val componentPaint = paint(13f, color = GRAY, italic = true)
    private val macroPaint = paint(12f, color = GRAY)
    private val tocPaint = paint(16f, color = LINK)
    private val totalsBoxPaint = Paint().apply { color = TOTALS_BG }

    fun render(): RenderedDiary {
        val nav = mutableListOf<PdfDayNav>()

        // ---- Table of contents (may span several pages) ----
        startPage()
        canvas.drawText(title, MARGIN, y + titlePaint.textSize, titlePaint)
        y += titlePaint.textSize + 8f
        canvas.drawText(rangeLabel, MARGIN, y + subtitlePaint.textSize, subtitlePaint)
        y += subtitlePaint.textSize + 24f

        data class TocLine(val label: String, val tocPageIndex: Int, val rect: RectF)

        val tocLines = mutableListOf<TocLine>()
        days.forEach { day ->
            val label = day.day.format(dateFormatter)
            val lineHeight = tocPaint.textSize + 14f
            ensureSpace(lineHeight)
            val top = y
            canvas.drawText(label, MARGIN, y + tocPaint.textSize, tocPaint)
            tocLines += TocLine(
                label = label,
                tocPageIndex = pageIndex,
                rect = RectF(MARGIN, top, MARGIN + tocPaint.measureText(label), top + tocPaint.textSize + 4f),
            )
            y += lineHeight
        }

        // ---- One section per day, each starting on a fresh page ----
        days.forEachIndexed { index, day ->
            startPage()
            val targetPage = pageIndex
            drawDay(day)
            val toc = tocLines[index]
            nav += PdfDayNav(
                label = toc.label,
                tocPageIndex = toc.tocPageIndex,
                tocRect = toc.rect,
                targetPageIndex = targetPage,
            )
        }

        finishPage()
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return RenderedDiary(bytes = out.toByteArray(), pageHeightPt = PAGE_H, days = nav)
    }

    private fun drawDay(day: TravelDay) {
        canvas.drawText(day.day.format(dateFormatter), MARGIN, y + dayHeaderPaint.textSize, dayHeaderPaint)
        y += dayHeaderPaint.textSize + 10f

        if (options.dailyTotals) {
            drawTotalsBox(day)
        }

        day.records.sortedBy { it.timestamp.toInstant() }.forEach { record ->
            drawMeal(record)
        }
    }

    /** A shaded band under the day header carrying the whole-day totals — visually above the meals. */
    private fun drawTotalsBox(day: TravelDay) {
        val valueLines = wrapItems(nutrientItems(sumNutrients(day.records)), totalsValuePaint, CONTENT_WIDTH - 2 * BOX_PADDING)
        val labelHeight = totalsLabelPaint.textSize + 6f
        val valuesHeight = valueLines.size * (totalsValuePaint.textSize + LINE_GAP)
        val boxHeight = BOX_PADDING + labelHeight + valuesHeight + BOX_PADDING

        ensureSpace(boxHeight)
        canvas.drawRoundRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + boxHeight, 8f, 8f, totalsBoxPaint)

        var ty = y + BOX_PADDING
        canvas.drawText("Daily totals", MARGIN + BOX_PADDING, ty + totalsLabelPaint.textSize, totalsLabelPaint)
        ty += labelHeight
        valueLines.forEach { line ->
            canvas.drawText(line, MARGIN + BOX_PADDING, ty + totalsValuePaint.textSize, totalsValuePaint)
            ty += totalsValuePaint.textSize + LINE_GAP
        }
        y += boxHeight + 14f
    }

    private fun drawMeal(record: Record) {
        val template = record.template
        val sized = resolveSizedPhotos(record)
        val rowWidth = if (sized.isEmpty()) {
            0f
        } else {
            sized.fold(0f) { acc, p -> acc + p.width } + PHOTO_GAP * (sized.size - 1)
        }
        val photoRowHeight = sized.maxOfOrNull { it.height } ?: 0f

        val descriptionBlock = template.description
            .takeIf { options.description && it.isNotBlank() }
            ?.let { TextBlock(it, bodyPaint) }
        // Components and macros are the only blocks that may sit beside the photo.
        val sideBlocks = buildList {
            if (options.components && template.mealComponents.isNotEmpty()) {
                val components = template.mealComponents.joinToString("\n") { comp ->
                    "• ${comp.name}" + comp.estimatedAmount.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                }
                add(TextBlock(components, componentPaint))
            }
            if (options.mealMacros) {
                add(
                    TextBlock(
                        text = "",
                        paint = macroPaint,
                        items = nutrientItems(template.nutrients),
                        gapBefore = SECTION_GAP,
                    ),
                )
            }
        }

        val titleBlock = TextBlock(heading(record), mealTitlePaint)

        // Photo + (components/macros) block: two columns when the photo is at most half the width.
        val fitsOneRow = sized.isNotEmpty() && rowWidth <= CONTENT_WIDTH
        val twoColumn = fitsOneRow && rowWidth <= HALF_WIDTH && sized.all { it.width <= HALF_WIDTH } &&
            sideBlocks.isNotEmpty()
        val sideWidth = CONTENT_WIDTH - rowWidth - COLUMN_GAP

        // Keep the whole record together on one page. Measure it; if it fits on a page but not in the
        // remaining space, move it to the next page. Only records taller than a full page are split.
        val recordHeight = measureMeal(titleBlock, descriptionBlock, sideBlocks, sized, twoColumn, photoRowHeight, sideWidth)
        if (recordHeight <= PAGE_H - 2 * MARGIN && y + recordHeight > PAGE_H - MARGIN) {
            startPage()
        }

        drawBlockPaginated(titleBlock)
        y += TITLE_GAP
        descriptionBlock?.let {
            drawBlockPaginated(it)
            y += POST_DESC_GAP
        }

        val twoColumnBlockHeight = maxOf(photoRowHeight, measureBlocks(sideBlocks, sideWidth))
        if (twoColumn && y + twoColumnBlockHeight <= PAGE_H - MARGIN) {
            val startY = y
            drawPhotoRow(sized, MARGIN, startY)
            drawBlocks(sideBlocks, MARGIN + rowWidth + COLUMN_GAP, startY, sideWidth)
            y = startY + twoColumnBlockHeight
        } else {
            if (sized.isNotEmpty()) drawPhotosWrapped(sized)
            sideBlocks.forEachIndexed { index, block ->
                if (index > 0) y += block.gapBefore
                drawBlockPaginated(block)
            }
        }
        y += MEAL_GAP
    }

    /** Total height of a whole meal in its chosen layout, used to keep records off page breaks. */
    private fun measureMeal(
        titleBlock: TextBlock,
        descriptionBlock: TextBlock?,
        sideBlocks: List<TextBlock>,
        sized: List<SizedPhoto>,
        twoColumn: Boolean,
        photoRowHeight: Float,
        sideWidth: Float,
    ): Float {
        var h = measureBlock(titleBlock, CONTENT_WIDTH) + TITLE_GAP
        if (descriptionBlock != null) {
            h += measureBlock(descriptionBlock, CONTENT_WIDTH) + POST_DESC_GAP
        }
        h += if (twoColumn) {
            maxOf(photoRowHeight, measureBlocks(sideBlocks, sideWidth))
        } else {
            (if (sized.isNotEmpty()) measurePhotosWrapped(sized) else 0f) +
                measureBlocks(sideBlocks, CONTENT_WIDTH)
        }
        return h
    }

    private fun measureBlock(block: TextBlock, maxWidth: Float): Float =
        linesFor(block, maxWidth).size * (block.paint.textSize + LINE_GAP)

    /** Height of [sized] laid out across the full content width, matching [drawPhotosWrapped]. */
    private fun measurePhotosWrapped(sized: List<SizedPhoto>): Float {
        var cx = MARGIN
        var rowHeight = 0f
        var stacked = 0f
        sized.forEach { p ->
            if (cx > MARGIN && cx + p.width > MARGIN + CONTENT_WIDTH) {
                stacked += rowHeight + PHOTO_GAP
                cx = MARGIN
                rowHeight = 0f
            }
            cx += p.width + PHOTO_GAP
            rowHeight = maxOf(rowHeight, p.height)
        }
        return 4f + stacked + rowHeight + 4f
    }

    private fun heading(record: Record): String {
        val time = record.timestamp.format(timeFormatter)
        return listOf(time, record.template.name).filter { it.isNotBlank() }.joinToString("   ")
    }

    private fun resolveSizedPhotos(record: Record): List<SizedPhoto> {
        val template = record.template
        val filenames = when (options.photos) {
            PdfPhotoMode.ALL -> template.imageFilenames
            PdfPhotoMode.TITULAR -> listOfNotNull(representativeImageFilename(template))
            PdfPhotoMode.NONE -> emptyList()
        }
        return filenames.mapNotNull { photos[it] }.map { bitmap ->
            var w = PHOTO_HEIGHT * (bitmap.width.toFloat() / bitmap.height)
            var h = PHOTO_HEIGHT
            if (w > CONTENT_WIDTH) {
                w = CONTENT_WIDTH
                h = CONTENT_WIDTH * (bitmap.height.toFloat() / bitmap.width)
            }
            SizedPhoto(bitmap, w, h)
        }
    }

    /** Draws photos left-to-right on a single row at [startY] (caller guarantees they fit). */
    private fun drawPhotoRow(sized: List<SizedPhoto>, x: Float, startY: Float) {
        var cx = x
        sized.forEach { p ->
            canvas.drawBitmap(p.bitmap, null, RectF(cx, startY, cx + p.width, startY + p.height), null)
            cx += p.width + PHOTO_GAP
        }
    }

    /** Draws photos across the full content width, wrapping to new rows and paginating as needed. */
    private fun drawPhotosWrapped(sized: List<SizedPhoto>) {
        y += 4f
        var cx = MARGIN
        var rowHeight = 0f
        sized.forEach { p ->
            if (cx > MARGIN && cx + p.width > MARGIN + CONTENT_WIDTH) {
                y += rowHeight + PHOTO_GAP
                cx = MARGIN
                rowHeight = 0f
            }
            ensureSpace(p.height)
            canvas.drawBitmap(p.bitmap, null, RectF(cx, y, cx + p.width, y + p.height), null)
            cx += p.width + PHOTO_GAP
            rowHeight = maxOf(rowHeight, p.height)
        }
        y += rowHeight + 4f
    }

    // ---- Text block helpers ----

    private fun measureBlocks(blocks: List<TextBlock>, maxWidth: Float): Float =
        blocks.foldIndexed(0f) { index, acc, b ->
            acc + (if (index > 0) b.gapBefore else 0f) + linesFor(b, maxWidth).size * (b.paint.textSize + LINE_GAP)
        }

    /** Draws [blocks] stacked at ([x], [startY]) within [maxWidth]. No pagination — caller ensures fit. */
    private fun drawBlocks(blocks: List<TextBlock>, x: Float, startY: Float, maxWidth: Float) {
        var ly = startY
        blocks.forEachIndexed { index, block ->
            if (index > 0) ly += block.gapBefore
            linesFor(block, maxWidth).forEach { line ->
                canvas.drawText(line, x, ly + block.paint.textSize, block.paint)
                ly += block.paint.textSize + LINE_GAP
            }
        }
    }

    /** Draws a single block at the full content width, paginating line by line. Advances [y]. */
    private fun drawBlockPaginated(block: TextBlock) {
        linesFor(block, CONTENT_WIDTH).forEach { line ->
            ensureSpace(block.paint.textSize + LINE_GAP)
            canvas.drawText(line, MARGIN, y + block.paint.textSize, block.paint)
            y += block.paint.textSize + LINE_GAP
        }
    }

    private fun linesFor(block: TextBlock, maxWidth: Float): List<String> =
        block.items?.let { wrapItems(it, block.paint, maxWidth) }
            ?: wrap(block.text, block.paint, maxWidth)

    /** Packs atomic [items] into lines, breaking only between items (never inside one). */
    private fun wrapItems(items: List<String>, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        items.forEach { item ->
            if (current.isEmpty()) {
                current.append(item)
            } else {
                val candidate = "$current$MACRO_SEPARATOR$item"
                if (paint.measureText(candidate) <= maxWidth) {
                    current = StringBuilder(candidate)
                } else {
                    lines += current.toString()
                    current = StringBuilder(item)
                }
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }

    /** Wraps [text] to [maxWidth], honouring explicit newlines and preferring to break at spaces. */
    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        text.split("\n").forEach { paragraph ->
            if (paragraph.isEmpty()) {
                lines += ""
                return@forEach
            }
            var start = 0
            while (start < paragraph.length) {
                val fit = paint.breakText(paragraph, start, paragraph.length, true, maxWidth, null)
                    .coerceAtLeast(1)
                var end = start + fit
                if (end < paragraph.length) {
                    val lastSpace = paragraph.lastIndexOf(' ', end - 1)
                    if (lastSpace > start) end = lastSpace
                }
                lines += paragraph.substring(start, end)
                start = end
                while (start < paragraph.length && paragraph[start] == ' ') start++
            }
        }
        return lines
    }

    private fun ensureSpace(needed: Float) {
        if (y + needed > PAGE_H - MARGIN) {
            startPage()
        }
    }

    private fun startPage() {
        finishPage()
        pageIndex += 1
        val info = PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), pageIndex + 1).create()
        page = doc.startPage(info)
        canvas = page!!.canvas
        y = MARGIN
    }

    private fun finishPage() {
        page?.let { doc.finishPage(it) }
        page = null
    }

    private fun nutrientItems(n: Nutrients): List<String> = listOf(
        "Calories " + (n.calories?.let { "$it kcal" } ?: EMPTY),
        "Protein " + grams(n.protein),
        "Fat " + grams(n.fat),
        "Sat. fat " + grams(n.ofWhichSaturated),
        "Carbs " + grams(n.carbs),
        "Sugar " + grams(n.ofWhichSugar),
        "Added sugar " + grams(n.ofWhichAddedSugar),
        "Salt " + grams(n.salt),
        "Fibre " + grams(n.fibre),
    )

    private fun grams(v: Float?): String =
        if (v == null) EMPTY else {
            val rounded = Math.round(v * 10f) / 10f
            if (rounded % 1f == 0f) "${rounded.toInt()} g" else "$rounded g"
        }

    /** Sums each nutrient across [records]; a nutrient stays null when no record reports it. */
    private fun sumNutrients(records: List<Record>): Nutrients {
        fun <T> sum(select: (Nutrients) -> T?, zero: T, plus: (T, T) -> T): T? {
            val present = records.mapNotNull { select(it.template.nutrients) }
            return if (present.isEmpty()) null else present.fold(zero, plus)
        }
        return Nutrients(
            calories = sum({ it.calories }, 0, Int::plus),
            protein = sum({ it.protein }, 0f, Float::plus),
            fat = sum({ it.fat }, 0f, Float::plus),
            ofWhichSaturated = sum({ it.ofWhichSaturated }, 0f, Float::plus),
            carbs = sum({ it.carbs }, 0f, Float::plus),
            ofWhichSugar = sum({ it.ofWhichSugar }, 0f, Float::plus),
            ofWhichAddedSugar = sum({ it.ofWhichAddedSugar }, 0f, Float::plus),
            salt = sum({ it.salt }, 0f, Float::plus),
            fibre = sum({ it.fibre }, 0f, Float::plus),
        )
    }

    private fun paint(
        size: Float,
        bold: Boolean = false,
        italic: Boolean = false,
        color: Int = Color.BLACK,
    ) = Paint().apply {
        isAntiAlias = true
        textSize = size
        this.color = color
        val style = when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        typeface = Typeface.create(Typeface.DEFAULT, style)
    }

    companion object {
        private const val PAGE_W = 595f   // A4 @ 72dpi
        private const val PAGE_H = 842f
        private const val MARGIN = 40f
        private const val CONTENT_WIDTH = PAGE_W - 2 * MARGIN
        private const val HALF_WIDTH = CONTENT_WIDTH / 2f
        private const val COLUMN_GAP = 14f
        private const val LINE_GAP = 4f
        private const val BLOCK_GAP = 3f
        private const val SECTION_GAP = 10f
        private const val TITLE_GAP = 9f
        private const val POST_DESC_GAP = 7f
        private const val MEAL_GAP = 16f
        private const val PHOTO_HEIGHT = 210f
        private const val PHOTO_GAP = 6f
        private const val BOX_PADDING = 12f
        private const val MACRO_SEPARATOR = "   ·   "
        private const val EMPTY = "—"
        private val GRAY = Color.rgb(90, 90, 90)
        private val NEAR_BLACK = Color.rgb(35, 35, 35)
        private val LINK = Color.rgb(20, 90, 200)
        private val TOTALS_BG = Color.rgb(238, 240, 244)
    }
}
