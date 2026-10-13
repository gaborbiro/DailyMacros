package dev.gaborbiro.dailymacros.features.shared.mealreminders

import android.util.Log
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
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
    private val recordsRepository: RecordsRepository,
    private val runner: MealReminderRunner,
) {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    @OptIn(FlowPreview::class)
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            runSafely()
            recordsRepository.observeRecords(searchTerm = null, sinceEpochMillis = 0L)
                // Skip the initial "current state" emission so we only react to changes.
                .drop(1)
                // Coalesce bursts of writes (e.g. an AI analysis result landing after the save).
                .debounce(DEBOUNCE_MS)
                .onEach { runSafely() }
                .collect {}
        }
    }

    /** For a settings change: apply it now rather than on the next diary change. */
    fun refresh() {
        scope.launch { runSafely() }
    }

    private suspend fun runSafely() {
        runCatching { runner.run() }
            .onFailure { Log.e(TAG, "Meal reminder pass failed", it) }
    }

    private companion object {
        const val TAG = "MealReminders"
        const val DEBOUNCE_MS = 5_000L
    }
}
