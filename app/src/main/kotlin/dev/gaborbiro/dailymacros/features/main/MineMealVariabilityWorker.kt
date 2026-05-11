package dev.gaborbiro.dailymacros.features.main

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.features.settings.SettingsPrefs
import dev.gaborbiro.dailymacros.features.settings.variability.MEAL_VARIABILITY_MINING_OUTPUT_ERROR
import dev.gaborbiro.dailymacros.features.settings.variability.MEAL_VARIABILITY_MINING_UNIQUE_WORK
import dev.gaborbiro.dailymacros.features.settings.variability.MineMealVariabilityPreviewUseCase
import dev.gaborbiro.dailymacros.util.CHANNEL_ID_FOREGROUND
import dev.gaborbiro.dailymacros.util.showTitleTextNotification
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.time.delay
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@HiltWorker
class MineMealVariabilityWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val mineMealVariabilityPreviewUseCase: MineMealVariabilityPreviewUseCase,
    private val settingsPrefs: SettingsPrefs,
    private val analyticsLogger: AnalyticsLogger,
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        return try {
            delay(0.5.seconds.toJavaDuration())
            setForegroundAsync(createForegroundInfo()).await()
            val preview = mineMealVariabilityPreviewUseCase.execute()
            val generatedAt = System.currentTimeMillis()
            settingsPrefs.variabilityMiningRequestJson = preview.requestJsonPretty
            settingsPrefs.variabilityMiningResponseJson = preview.responseJsonPretty
            settingsPrefs.variabilityMiningGeneratedAtEpochMs = generatedAt
            settingsPrefs.variabilityMiningRequestJsonExpansionBits = ""
            settingsPrefs.variabilityMiningResponseJsonExpansionBits = ""
            settingsPrefs.variabilityMiningRequestJsonSectionExpanded = false
            settingsPrefs.variabilityMiningResponseJsonSectionExpanded = false
            val successTitle = if (preview.skippedNoNewObservations) {
                applicationContext.getString(R.string.variability_mining_skipped_title)
            } else {
                applicationContext.getString(R.string.variability_mining_success_title)
            }
            val successText = if (preview.skippedNoNewObservations) {
                applicationContext.getString(R.string.variability_mining_skipped_text)
            } else {
                applicationContext.getString(R.string.variability_mining_success_text)
            }
            applicationContext.showTitleTextNotification(
                id = NOTIFICATION_ID_RESULT,
                title = successTitle,
                text = successText,
                isError = false,
            )
            Result.success()
        } catch (t: Throwable) {
            analyticsLogger.logError(t)
            val message = t.message?.takeIf { it.isNotBlank() } ?: t.toString()
            applicationContext.showTitleTextNotification(
                id = NOTIFICATION_ID_RESULT,
                title = applicationContext.getString(R.string.variability_mining_failure_title),
                text = message,
                isError = true,
            )
            Result.failure(
                workDataOf(MEAL_VARIABILITY_MINING_OUTPUT_ERROR to message),
            )
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification =
            NotificationCompat.Builder(applicationContext, CHANNEL_ID_FOREGROUND)
                .setContentTitle(applicationContext.getString(R.string.variability_mining_foreground_title))
                .setSmallIcon(R.drawable.ic_nutrition)
                .setOngoing(true)
                .build()

        return ForegroundInfo(
            FOREGROUND_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 124_010
        private const val NOTIFICATION_ID_RESULT = 124_011

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<MineMealVariabilityWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                MEAL_VARIABILITY_MINING_UNIQUE_WORK,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
