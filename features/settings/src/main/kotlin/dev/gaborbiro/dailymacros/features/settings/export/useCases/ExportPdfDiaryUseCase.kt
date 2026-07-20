package dev.gaborbiro.dailymacros.features.settings.export.useCases

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.scale
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.common.utils.diaryDayStartTime
import dev.gaborbiro.dailymacros.features.common.utils.diaryDayWindowStart
import dev.gaborbiro.dailymacros.features.common.utils.logicalDiaryDate
import dev.gaborbiro.dailymacros.features.settings.R
import dev.gaborbiro.dailymacros.features.settings.export.CreatePublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.settings.export.StreamWriter
import dev.gaborbiro.dailymacros.features.settings.export.pdf.DiaryDateRange
import dev.gaborbiro.dailymacros.features.settings.export.pdf.PdfDiaryRenderer
import dev.gaborbiro.dailymacros.features.settings.export.pdf.PdfNavigationDecorator
import dev.gaborbiro.dailymacros.features.settings.export.pdf.representativeImageFilename
import dev.gaborbiro.dailymacros.features.shared.model.TravelDay
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PdfExportOptions
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PdfPhotoMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

sealed interface PdfExportResult {
    data class Success(val uri: Uri) : PdfExportResult

    /** No records fall in the chosen range. */
    data object Empty : PdfExportResult

    /** User dismissed the save-location picker. */
    data object Cancelled : PdfExportResult
}

class ExportPdfDiaryUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val recordRepository: RecordsRepository,
    private val imageStore: ImageStore,
    private val streamWriter: StreamWriter,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun execute(
        createPublicDocumentUseCase: CreatePublicDocumentUseCase,
        range: DiaryDateRange,
        options: PdfExportOptions,
    ): PdfExportResult {
        settingsRepository.setPdfExportOptions(options)

        val zone = ZoneId.systemDefault()
        val dayStart = diaryDayStartTime(settingsRepository.getDiaryDayStartHour())
        val windowStart = diaryDayWindowStart(range.from, dayStart, zone)
        val windowEndExclusive = diaryDayWindowStart(range.to.plusDays(1), dayStart, zone)

        val records = recordRepository.getRecords(since = windowStart)
            .filter { it.timestamp.toInstant() < windowEndExclusive.toInstant() }

        if (records.isEmpty()) return PdfExportResult.Empty

        // Ask for the save location up front (cheap) so the user isn't left waiting on the render
        // before the picker appears; only then do the heavy photo loading and drawing.
        val fileName = "food-diary-${range.from}_to_${range.to}.pdf"
        val uri = createPublicDocumentUseCase.execute(fileName) ?: return PdfExportResult.Cancelled

        val days = records.groupByDiaryDay(dayStart)
        val photos = loadPhotos(days.flatMap { it.records }, options)

        val locale = Locale.getDefault()
        val rangeLabel = formatRangeLabel(range, locale)
        val renderer = PdfDiaryRenderer(
            title = appContext.getString(R.string.pdf_export_title),
            rangeLabel = rangeLabel,
            days = days,
            options = options,
            photos = photos,
            locale = locale,
        )
        val rendered = withContext(Dispatchers.Default) { renderer.render() }

        streamWriter.execute(uri) { output ->
            PDFBoxResourceLoader.init(appContext)
            PdfNavigationDecorator.writeDecorated(rendered, output)
        }
        return PdfExportResult.Success(uri)
    }

    private suspend fun loadPhotos(
        records: List<Record>,
        options: PdfExportOptions,
    ): Map<String, Bitmap> = withContext(Dispatchers.IO) {
        if (options.photos == PdfPhotoMode.NONE) return@withContext emptyMap()

        val filenames = records.flatMap { record ->
            when (options.photos) {
                PdfPhotoMode.ALL -> record.template.imageFilenames
                PdfPhotoMode.TITULAR -> listOfNotNull(representativeImageFilename(record.template))
                PdfPhotoMode.NONE -> emptyList()
            }
        }.distinct()

        buildMap {
            filenames.forEach { filename ->
                val bitmap = runCatching { imageStore.read(filename, thumbnail = false) }.getOrNull()
                    ?: runCatching { imageStore.read(filename, thumbnail = true) }.getOrNull()
                if (bitmap != null) put(filename, bitmap.toPrintBitmap(PHOTO_MAX_EDGE_PX))
            }
        }
    }

    /**
     * Produces a software (ARGB_8888) bitmap downscaled to [maxEdgePx]. ImageStore may return a
     * hardware bitmap (Config.HARDWARE via ImageDecoder), which PdfDocument's software Canvas cannot
     * draw ("Software rendering doesn't support hardware bitmaps"), so convert before scaling.
     */
    private fun Bitmap.toPrintBitmap(maxEdgePx: Int): Bitmap {
        val software = if (config == Bitmap.Config.HARDWARE) {
            copy(Bitmap.Config.ARGB_8888, false) ?: this
        } else {
            this
        }
        val longest = maxOf(software.width, software.height)
        if (longest <= maxEdgePx) return software
        val ratio = maxEdgePx.toFloat() / longest
        return software.scale(
            (software.width * ratio).toInt().coerceAtLeast(1),
            (software.height * ratio).toInt().coerceAtLeast(1),
        )
    }

    private fun List<Record>.groupByDiaryDay(dayStart: java.time.LocalTime): List<TravelDay> =
        sortedBy { it.timestamp.toInstant() }
            .groupByTo(sortedMapOf(), { it.timestamp.logicalDiaryDate(dayStart) }, { it })
            .map { (day, dayRecords) ->
                TravelDay(
                    records = dayRecords.toList(),
                    day = day,
                    firstLog = dayRecords.minBy { it.timestamp.toInstant() }.timestamp,
                    lastLog = dayRecords.maxBy { it.timestamp.toInstant() }.timestamp,
                    diaryDayStart = dayStart,
                )
            }

    private fun formatRangeLabel(range: DiaryDateRange, locale: Locale): String {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        return if (range.from == range.to) {
            range.from.format(formatter)
        } else {
            "${range.from.format(formatter)} – ${range.to.format(formatter)}"
        }
    }

    companion object {
        private const val PHOTO_MAX_EDGE_PX = 1000
    }
}
