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
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PdfTextMode
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
 */
class PdfDiaryRenderer(
    private val title: String,
    private val rangeLabel: String,
    private val days: List<TravelDay>,
    private val options: PdfExportOptions,
    private val photos: Map<String, Bitmap>,
    private val locale: Locale = Locale.getDefault(),
) {

    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)

    private val doc = PdfDocument()
    private var page: PdfDocument.Page? = null
    private var canvas: Canvas = Canvas()
    private var pageIndex = -1
    private var y = MARGIN

    private val titlePaint = paint(24f, bold = true)
    private val subtitlePaint = paint(12f, color = GRAY)
    private val dayHeaderPaint = paint(18f, bold = true)
    private val totalsLabelPaint = paint(11f, bold = true)
    private val mealTitlePaint = paint(13f, bold = true)
    private val bodyPaint = paint(11f)
    private val macroPaint = paint(10f, color = GRAY)
    private val tocPaint = paint(13f, color = LINK)

    fun render(): RenderedDiary {
        val nav = mutableListOf<PdfDayNav>()

        // ---- Table of contents (may span several pages) ----
        startPage()
        canvas.drawText(title, MARGIN, y + titlePaint.textSize, titlePaint)
        y += titlePaint.textSize + 8f
        canvas.drawText(rangeLabel, MARGIN, y + subtitlePaint.textSize, subtitlePaint)
        y += subtitlePaint.textSize + 20f

        data class TocLine(val label: String, val tocPageIndex: Int, val rect: RectF)

        val tocLines = mutableListOf<TocLine>()
        days.forEach { day ->
            val label = day.day.format(dateFormatter)
            val lineHeight = tocPaint.textSize + 12f
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
            val totals = sumNutrients(day.records)
            drawWrapped("Daily totals", totalsLabelPaint)
            drawWrapped(formatNutrients(totals), macroPaint)
            y += 8f
        }

        day.records.sortedBy { it.timestamp.toInstant() }.forEach { record ->
            drawMeal(record)
            y += 10f
        }
    }

    private fun drawMeal(record: Record) {
        val template = record.template
        val time = record.timestamp.format(timeFormatter)

        ensureSpace(mealTitlePaint.textSize + 4f)
        val heading = if (options.text == PdfTextMode.TITLE_ONLY || template.name.isNotBlank()) {
            "$time   ${template.name}".trim()
        } else {
            time
        }
        drawWrapped(heading, mealTitlePaint)

        if (options.text == PdfTextMode.TITLE_AND_DESCRIPTION) {
            if (template.description.isNotBlank()) {
                drawWrapped(template.description, bodyPaint)
            }
            if (template.mealComponents.isNotEmpty()) {
                val components = template.mealComponents.joinToString("\n") { comp ->
                    "• ${comp.name}" + comp.estimatedAmount.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                }
                drawWrapped(components, bodyPaint)
            }
        }

        if (options.photos != PdfPhotoMode.NONE) {
            drawPhotos(record)
        }

        if (options.mealMacros) {
            drawWrapped(formatNutrients(template.nutrients), macroPaint)
        }
    }

    private fun drawPhotos(record: Record) {
        val template = record.template
        val filenames = when (options.photos) {
            PdfPhotoMode.ALL -> template.imageFilenames
            PdfPhotoMode.TITULAR -> listOfNotNull(representativeImageFilename(template))
            PdfPhotoMode.NONE -> emptyList()
        }
        val bitmaps = filenames.mapNotNull { photos[it] }
        if (bitmaps.isEmpty()) return

        y += 4f
        var x = MARGIN
        var rowHeight = 0f
        bitmaps.forEach { bitmap ->
            var w = PHOTO_HEIGHT * (bitmap.width.toFloat() / bitmap.height)
            var h = PHOTO_HEIGHT
            if (w > CONTENT_WIDTH) {
                w = CONTENT_WIDTH
                h = CONTENT_WIDTH * (bitmap.height.toFloat() / bitmap.width)
            }
            // Wrap to a new row when this photo would overflow the content width.
            if (x > MARGIN && x + w > MARGIN + CONTENT_WIDTH) {
                y += rowHeight + PHOTO_GAP
                x = MARGIN
                rowHeight = 0f
            }
            ensureSpace(h)
            if (x == MARGIN) {
                // ensureSpace may have moved us to a fresh page; nothing else to reset.
            }
            val dest = RectF(x, y, x + w, y + h)
            canvas.drawBitmap(bitmap, null, dest, null)
            x += w + PHOTO_GAP
            rowHeight = maxOf(rowHeight, h)
        }
        y += rowHeight + 4f
    }

    /** Wraps [text] to the content width, honouring explicit newlines, and advances [y]. */
    private fun drawWrapped(text: String, paint: Paint) {
        if (text.isEmpty()) return
        text.split("\n").forEach { paragraph ->
            if (paragraph.isEmpty()) {
                y += paint.textSize + LINE_GAP
                return@forEach
            }
            var start = 0
            while (start < paragraph.length) {
                val fit = paint.breakText(paragraph, start, paragraph.length, true, CONTENT_WIDTH, null)
                    .coerceAtLeast(1)
                var end = start + fit
                // Prefer to break at the last space so words aren't split mid-way.
                if (end < paragraph.length) {
                    val lastSpace = paragraph.lastIndexOf(' ', end - 1)
                    if (lastSpace > start) end = lastSpace
                }
                ensureSpace(paint.textSize + LINE_GAP)
                canvas.drawText(paragraph, start, end, MARGIN, y + paint.textSize, paint)
                y += paint.textSize + LINE_GAP
                start = end
                while (start < paragraph.length && paragraph[start] == ' ') start++
            }
        }
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

    private fun formatNutrients(n: Nutrients): String = buildList {
        add("Calories " + (n.calories?.let { "$it kcal" } ?: EMPTY))
        add("Protein " + grams(n.protein))
        add("Fat " + grams(n.fat))
        add("Sat. fat " + grams(n.ofWhichSaturated))
        add("Carbs " + grams(n.carbs))
        add("Sugar " + grams(n.ofWhichSugar))
        add("Added sugar " + grams(n.ofWhichAddedSugar))
        add("Salt " + grams(n.salt))
        add("Fibre " + grams(n.fibre))
    }.joinToString("   ·   ")

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

    private fun paint(size: Float, bold: Boolean = false, color: Int = Color.BLACK) = Paint().apply {
        isAntiAlias = true
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    companion object {
        private const val PAGE_W = 595f   // A4 @ 72dpi
        private const val PAGE_H = 842f
        private const val MARGIN = 40f
        private const val CONTENT_WIDTH = PAGE_W - 2 * MARGIN
        private const val LINE_GAP = 4f
        private const val PHOTO_HEIGHT = 150f
        private const val PHOTO_GAP = 6f
        private const val EMPTY = "—"
        private val GRAY = Color.rgb(90, 90, 90)
        private val LINK = Color.rgb(20, 90, 200)
    }
}
