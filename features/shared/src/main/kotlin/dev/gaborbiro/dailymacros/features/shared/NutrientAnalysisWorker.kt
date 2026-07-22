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

        fun setWorkRequest(
            appContext: Context,
            recordId: Long,
            force: Boolean,
        ) {
            val policy = if (force) {
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            } else {
                ExistingPeriodicWorkPolicy.KEEP
            }
            val workRequest = PeriodicWorkRequestBuilder<NutrientAnalysisWorker>(
                repeatInterval = 15.minutes.toJavaDuration()
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    Data.Builder()
                        .putLong(ARGS_RECORD_ID, recordId)
                        .build()
                )
                .build()
            WorkManager.getInstance(appContext)
                .enqueueUniquePeriodicWork(getWorkName(recordId), policy, workRequest)
        }

        fun cancelWorkRequest(appContext: Context, recordId: Long) {
            WorkManager.getInstance(appContext).cancelUniqueWork(getWorkName(recordId))
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
            Result.failure()
        }
    }
}
