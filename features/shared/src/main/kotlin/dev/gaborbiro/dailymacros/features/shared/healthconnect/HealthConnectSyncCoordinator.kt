package dev.gaborbiro.dailymacros.features.shared.healthconnect

import android.util.Log
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subscribes to the records repository for the lifetime of the app process (same shape as
 * [dev.gaborbiro.dailymacros.features.widgets.WidgetAutoReloader]) and pushes changed entries to
 * Health Connect whenever the diary changes, if the user has turned the feature on. No
 * WorkManager involved - a Health Connect write is a local Binder call, not a network request,
 * so there's nothing to gain from a background scheduler here, same reasoning as the widget
 * reloader.
 *
 * Only pushes the last [SYNC_WINDOW_DAYS] days on every trigger, not the whole diary: Health
 * Connect's own clientRecordId/version upsert (see [HealthConnectSyncUseCase]) makes an
 * unbounded full resync *correct*, but its cost still grows with total diary size on every single
 * edit, forever. A rolling window covers new entries and realistic corrections (fixing a meal
 * logged minutes/hours ago) without that unbounded growth.
 *
 * Bootstrap once from `App.onCreate` via a Hilt entry point, unconditionally - the setting is
 * checked on every debounced tick, so toggling it on/off in Settings takes effect immediately
 * without needing to restart or re-arm anything.
 */
@Singleton
class HealthConnectSyncCoordinator @Inject constructor(
    private val recordsRepository: RecordsRepository,
    private val settingsRepository: SettingsRepository,
    private val healthConnectSyncUseCase: HealthConnectSyncUseCase,
) {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    @OptIn(FlowPreview::class)
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            recordsRepository.observeRecords(searchTerm = null, sinceEpochMillis = 0L)
                // Skip the initial "current state" emission so we only react to changes.
                .drop(1)
                // Coalesce bursts of writes (e.g. saving a record cascades to multiple tables,
                // or an AI analysis result lands moments after the initial save).
                .debounce(DEBOUNCE_MS)
                .onEach { sync() }
                .collect {}
        }
    }

    private suspend fun sync() {
        if (!settingsRepository.getHealthConnectSyncEnabled()) return
        val since = ZonedDateTime.now().minusDays(SYNC_WINDOW_DAYS)
        when (val result = healthConnectSyncUseCase.execute(since)) {
            is HealthConnectSyncResult.Success ->
                Log.i(TAG, "Synced ${result.count} record(s) to Health Connect")
            is HealthConnectSyncResult.Error ->
                Log.e(TAG, "Health Connect sync failed: ${result.message}")
            HealthConnectSyncResult.NotAvailable, HealthConnectSyncResult.PermissionRequired ->
                Log.w(TAG, "Health Connect sync skipped: $result")
        }
    }

    private companion object {
        private const val TAG = "HealthConnectSync"
        private const val DEBOUNCE_MS = 5_000L
        private const val SYNC_WINDOW_DAYS = 30L
    }
}
