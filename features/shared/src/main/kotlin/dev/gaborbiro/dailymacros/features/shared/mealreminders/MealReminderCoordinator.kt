package dev.gaborbiro.dailymacros.features.shared.mealreminders

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the meal reminder schedule in step with the diary for the lifetime of the app process
 * (same shape as HealthConnectSyncCoordinator): one pass on app start, which also re-arms after a
 * reboot, timezone or day-start change, then one after every burst of diary changes, so logging
 * lunch clears its reminder and a late breakfast moves lunch's.
 *
 * Bootstrap once from `App.onCreate`; the on/off switch is checked on every pass.
 */
@Singleton
class MealReminderCoordinator @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val recordsRepository: RecordsRepository,
    private val runner: MealReminderRunner,
) : MealReminderRefresher {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    @OptIn(FlowPreview::class)
    fun start() {
        if (job?.isActive == true) return
        // The first build scheduled reminders through WorkManager; drop any job it left behind.
        WorkManager.getInstance(appContext).cancelUniqueWork(LEGACY_WORK_NAME)
        job = scope.launch {
            runSafely(trigger = "app start")
            recordsRepository.observeRecords(searchTerm = null, sinceEpochMillis = 0L)
                // A failed query would otherwise end the subscription, silently freezing the
                // schedule until the next app start.
                .retryWhen { cause, _ ->
                    Log.e(TAG, "Diary observer failed, resubscribing", cause)
                    delay(DEBOUNCE_MS)
                    true
                }
                // Skip the initial "current state" emission so we only react to changes.
                .drop(1)
                // Coalesce bursts of writes (e.g. an AI analysis result landing after the save).
                .debounce(DEBOUNCE_MS)
                .onEach { runSafely(trigger = "diary change") }
                .collect {}
        }
    }

    /** Applies a change now (a setting, a moved meal) rather than waiting for the diary observer. */
    override fun refresh(trigger: String) {
        scope.launch { runSafely(trigger = trigger) }
    }

    private suspend fun runSafely(trigger: String) {
        runCatching { runner.run(trigger = trigger) }
            .onFailure { Log.e(TAG, "Meal reminder pass failed", it) }
    }

    private companion object {
        const val TAG = "MealReminders"
        const val DEBOUNCE_MS = 5_000L
        const val LEGACY_WORK_NAME = "meal_reminders"
    }
}
