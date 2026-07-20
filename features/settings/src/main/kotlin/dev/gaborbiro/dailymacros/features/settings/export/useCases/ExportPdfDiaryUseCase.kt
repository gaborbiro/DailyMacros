package dev.gaborbiro.dailymacros.features.settings.export.useCases

import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.features.settings.export.CreatePublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.settings.export.ExportPdfWorker
import dev.gaborbiro.dailymacros.features.settings.export.pdf.DiaryDateRange
import dev.gaborbiro.dailymacros.features.settings.export.pdf.PdfDiaryGenerator
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PdfExportOptions
import javax.inject.Inject

sealed interface PdfExportResult {
    /** The export was handed off to the background worker; the user will get a notification. */
    data object Enqueued : PdfExportResult

    /** No records fall in the chosen range. */
    data object Empty : PdfExportResult

    /** User dismissed the save-location picker. */
    data object Cancelled : PdfExportResult
}

/**
 * UI-side of the export: persists the chosen options, lets the user pick a save location (SAF), then
 * hands the actual generation to [ExportPdfWorker] so it survives the app being closed. Only the
 * (cheap) emptiness check and the picker run here; nothing heavy.
 */
class ExportPdfDiaryUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val pdfDiaryGenerator: PdfDiaryGenerator,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun execute(
        createPublicDocumentUseCase: CreatePublicDocumentUseCase,
        range: DiaryDateRange,
        options: PdfExportOptions,
    ): PdfExportResult {
        settingsRepository.setPdfExportOptions(options)

        if (pdfDiaryGenerator.recordsInRange(range).isEmpty()) return PdfExportResult.Empty

        val fileName = "food-diary-${range.from}_to_${range.to}.pdf"
        val uri = createPublicDocumentUseCase.execute(fileName) ?: return PdfExportResult.Cancelled

        // Persist the grant so the worker can still write to the file if the process is restarted.
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }.onFailure { Log.w(TAG, "Could not persist uri permission for $uri", it) }

        ExportPdfWorker.enqueue(appContext, uri, range, options)
        return PdfExportResult.Enqueued
    }

    companion object {
        private const val TAG = "ExportPdfDiaryUseCase"
    }
}
