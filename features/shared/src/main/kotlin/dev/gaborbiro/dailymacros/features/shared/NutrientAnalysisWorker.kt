package dev.gaborbiro.dailymacros.features.shared

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.features.shared.notifications.CHANNEL_ID_FOREGROUND
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.time.delay
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
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

        suspend fun setWorkRequest(
            appContext: Context,
            recordId: Long,
            force: Boolean,
        ) {
            if (force) {
                cancelWorkRequest(
                    appContext = appContext,
                    recordId = recordId,
                )
            }
            val works = runCatching {
                WorkManager.getInstance(appContext)
                    .getWorkInfosByTag(getTag(recordId))
                    .await()
            }.getOrElse { emptyList() }
            if (works.none { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }) {
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
                    .addTag(getTag(recordId))
                    .build()
                WorkManager.getInstance(appContext).enqueue(workRequest)
            }
        }

        fun cancelWorkRequest(appContext: Context, recordId: Long) {
            runCatching {
                WorkManager.getInstance(appContext).cancelAllWorkByTag(getTag(recordId))
            }
        }

        private fun getTag(recordId: Long): String {
            return "fetch_$recordId"
        }
    }

    override suspend fun doWork(): Result {
        return try {
            delay(.5.seconds.toJavaDuration())
            setForegroundAsync(createForegroundInfo()).await()
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

    private fun createForegroundInfo(): ForegroundInfo {
        val notification =
            NotificationCompat.Builder(applicationContext, CHANNEL_ID_FOREGROUND)
                .setContentTitle("Fetching macros…")
                .setSmallIcon(R.drawable.ic_nutrition)
                .setOngoing(true)
                .build()

        return ForegroundInfo(
            /* notificationId = */ 1,
            /* notification = */ notification,
            /* foregroundServiceType = */ ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }
}
