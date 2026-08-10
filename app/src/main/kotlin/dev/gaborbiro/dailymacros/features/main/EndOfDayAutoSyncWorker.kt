package dev.gaborbiro.dailymacros.features.main

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.gaborbiro.dailymacros.features.common.utils.diaryDayStartTime
import dev.gaborbiro.dailymacros.features.common.utils.nextDiaryDayBoundary
import dev.gaborbiro.dailymacros.features.settings.export.useCases.AutoSyncUseCase
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.util.cancelAutoSyncNotifications
import dev.gaborbiro.dailymacros.util.showAutoSyncConflictNotification
import dev.gaborbiro.dailymacros.util.showAutoSyncFailureNotification
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Catches edits made during a session that would otherwise sit unsynced until the next app open
 * (which may be days away, or never). Scheduled fresh on every [MainActivity.onResume] for the
 * next diary-day boundary; deliberately one-off, not periodic - if the app isn't reopened, nothing
 * reschedules the day after this one, matching the app's "no work without an app open" model.
 */
@HiltWorker
class EndOfDayAutoSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val autoSyncUseCase: AutoSyncUseCase,
) : CoroutineWorker(appContext, workerParameters) {

    companion object {
        private const val WORK_NAME = "end_of_day_auto_sync"

        fun schedule(appContext: Context, settingsRepository: SettingsRepository) {
            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            val dayStart = diaryDayStartTime(settingsRepository.getDiaryDayStartHour())
            val delayMs = Duration.between(now, nextDiaryDayBoundary(now, dayStart)).toMillis()
            val networkType = if (settingsRepository.getWifiOnlyBackupEnabled()) {
                NetworkType.UNMETERED
            } else {
                NetworkType.CONNECTED
            }
            val request = OneTimeWorkRequestBuilder<EndOfDayAutoSyncWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(networkType)
                        .build()
                )
                .build()
            WorkManager.getInstance(appContext)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }

    override suspend fun doWork(): Result {
        when (val result = autoSyncUseCase.execute()) {
            is AutoSyncUseCase.Result.ConflictDetected ->
                if (result.shouldNotify) applicationContext.showAutoSyncConflictNotification()

            is AutoSyncUseCase.Result.Failure ->
                if (result.shouldNotify) applicationContext.showAutoSyncFailureNotification()

            AutoSyncUseCase.Result.Success,
            AutoSyncUseCase.Result.Skipped,
            -> applicationContext.cancelAutoSyncNotifications()
        }
        return Result.success()
    }
}
