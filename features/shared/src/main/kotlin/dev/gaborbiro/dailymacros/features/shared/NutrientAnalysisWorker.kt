package dev.gaborbiro.dailymacros.features.shared

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import dev.gaborbiro.dailymacros.repositories.common.model.UsageLimitException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@HiltWorker
class NutrientAnalysisWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted private val workerParameters: WorkerParameters,
    private val nutrientAnalysisUseCase: NutrientAnalysisUseCase,
    private val analyticsLogger: AnalyticsLogger,
) : CoroutineWorker(appContext, workerParameters) {

    companion object {
        private const val ARGS_RECORD_ID = "record_id"

        /**
         * Shared by every instance so a single usage-limit hit can cancel retries
         * for all pending records at once, not just the one that hit the limit.
         */
        private const val WORK_TAG = "nutrient_analysis"

        /**
         * @param wifiOnly Whether this request should wait for Wi-Fi. Callers pass true only for
         * the automatic retry scheduled after a connectivity failure (gated by the user's "Wi-Fi
         * only for macro analysis" setting); every direct user action (saving/editing a record,
         * tapping "Re-run nutrient analysis", confirming an auto-detected photo entry) passes false so it
         * fires on any connection.
         */
        fun setWorkRequest(
            appContext: Context,
            recordId: Long,
            force: Boolean,
            wifiOnly: Boolean,
        ) {
            val policy = if (force) {
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            } else {
                ExistingPeriodicWorkPolicy.KEEP
            }
            val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            val workRequest = PeriodicWorkRequestBuilder<NutrientAnalysisWorker>(
                repeatInterval = 15.minutes.toJavaDuration()
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(networkType)
                        .build()
                )
                .setInputData(
                    Data.Builder()
                        .putLong(ARGS_RECORD_ID, recordId)
                        .build()
                )
                .addTag(WORK_TAG)
                .build()
            WorkManager.getInstance(appContext)
                .enqueueUniquePeriodicWork(getWorkName(recordId), policy, workRequest)
        }

        fun cancelWorkRequest(appContext: Context, recordId: Long) {
            WorkManager.getInstance(appContext).cancelUniqueWork(getWorkName(recordId))
        }

        /**
         * Stops every scheduled/retrying analysis job, not just the one that failed.
         * Used when a usage limit is hit so the remaining jobs don't keep re-hitting
         * it every 15 minutes; the user can still re-trigger analysis per entry.
         */
        fun cancelAllWorkRequests(appContext: Context) {
            WorkManager.getInstance(appContext).cancelAllWorkByTag(WORK_TAG)
        }

        private fun getWorkName(recordId: Long): String = "nutrient_analysis_$recordId"
    }

    override suspend fun doWork(): Result {
        return try {
            val recordId = workerParameters.inputData
                .getLong(ARGS_RECORD_ID, -1L)
            if (recordId == -1L) {
                Result.failure()
            } else {
                nutrientAnalysisUseCase.execute(
                    recordId = recordId,
                    notifyOnFailure = true,
                )
                cancelWorkRequest(
                    appContext = applicationContext,
                    recordId = recordId,
                )
                Result.success()
            }
        } catch (t: Throwable) {
            analyticsLogger.logError(t)
            if (t is DomainError.DisplayMessageToUser.OperationFailed && t.cause is UsageLimitException) {
                cancelAllWorkRequests(applicationContext)
            }
            Result.failure()
        }
    }
}
