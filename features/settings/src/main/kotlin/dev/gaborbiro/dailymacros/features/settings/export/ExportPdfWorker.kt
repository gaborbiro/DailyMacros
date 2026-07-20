package dev.gaborbiro.dailymacros.features.settings.export

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.gaborbiro.dailymacros.features.settings.export.pdf.DiaryDateRange
import dev.gaborbiro.dailymacros.features.settings.export.pdf.PdfDiaryGenerator
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PdfExportOptions
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PdfPhotoMode
import java.time.LocalDate

/**
 * Generates the food-diary PDF off the UI so it completes even if the user leaves the app. Posts a
 * progress notification, then a completion (Open / Share) or error notification. On failure the
 * empty file that SAF created at pick time is deleted so no 0-byte file is left behind.
 */
@HiltWorker
class ExportPdfWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val generator: PdfDiaryGenerator,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uri = inputData.getString(KEY_URI)?.let(Uri::parse) ?: return Result.failure()
        val from = inputData.getLong(KEY_FROM_EPOCH_DAY, -1L)
        val to = inputData.getLong(KEY_TO_EPOCH_DAY, -1L)
        if (from < 0 || to < 0) return Result.failure()
        val range = DiaryDateRange(LocalDate.ofEpochDay(from), LocalDate.ofEpochDay(to))
        val options = PdfExportOptions(
            dailyTotals = inputData.getBoolean(KEY_DAILY_TOTALS, true),
            photos = runCatching { PdfPhotoMode.valueOf(inputData.getString(KEY_PHOTOS) ?: "") }
                .getOrDefault(PdfPhotoMode.TITULAR),
            mealMacros = inputData.getBoolean(KEY_MEAL_MACROS, true),
            description = inputData.getBoolean(KEY_DESCRIPTION, true),
            components = inputData.getBoolean(KEY_COMPONENTS, true),
        )

        val notificationId = uri.toString().hashCode()
        ExportNotifications.showProgress(applicationContext, notificationId)

        return runCatching { generator.generate(uri, range, options) }
            .fold(
                onSuccess = {
                    ExportNotifications.showComplete(applicationContext, notificationId, uri)
                    Result.success()
                },
                onFailure = { t ->
                    Log.e(TAG, "PDF export failed", t)
                    // Remove the empty file SAF created when the user picked the location.
                    runCatching { DocumentsContract.deleteDocument(applicationContext.contentResolver, uri) }
                    ExportNotifications.showError(applicationContext, notificationId)
                    Result.failure()
                },
            )
    }

    companion object {
        private const val TAG = "ExportPdfWorker"
        private const val KEY_URI = "uri"
        private const val KEY_FROM_EPOCH_DAY = "from"
        private const val KEY_TO_EPOCH_DAY = "to"
        private const val KEY_DAILY_TOTALS = "daily_totals"
        private const val KEY_PHOTOS = "photos"
        private const val KEY_MEAL_MACROS = "meal_macros"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_COMPONENTS = "components"

        fun enqueue(context: Context, uri: Uri, range: DiaryDateRange, options: PdfExportOptions) {
            val data = Data.Builder()
                .putString(KEY_URI, uri.toString())
                .putLong(KEY_FROM_EPOCH_DAY, range.from.toEpochDay())
                .putLong(KEY_TO_EPOCH_DAY, range.to.toEpochDay())
                .putBoolean(KEY_DAILY_TOTALS, options.dailyTotals)
                .putString(KEY_PHOTOS, options.photos.name)
                .putBoolean(KEY_MEAL_MACROS, options.mealMacros)
                .putBoolean(KEY_DESCRIPTION, options.description)
                .putBoolean(KEY_COMPONENTS, options.components)
                .build()

            val request = OneTimeWorkRequestBuilder<ExportPdfWorker>()
                .setInputData(data)
                .build()

            // Unique per target file so re-picking the same file replaces a stale attempt.
            WorkManager.getInstance(context).enqueueUniqueWork(
                "pdf_export_${uri.toString().hashCode()}",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
