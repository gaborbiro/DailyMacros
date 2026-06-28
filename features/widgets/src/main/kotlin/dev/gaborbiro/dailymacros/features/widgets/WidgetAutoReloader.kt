package dev.gaborbiro.dailymacros.features.widgets

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.features.widgets.diary.DiaryWidgetScreen
import dev.gaborbiro.dailymacros.features.widgets.quickpick.QuickPickReloadWorker
import dev.gaborbiro.dailymacros.features.widgets.quickpick.QuickPickWidgetScreen
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subscribes to the records repository for the lifetime of the app process and triggers a
 * widget reload whenever underlying data changes. With this in place, no caller needs to
 * explicitly schedule widget reloads after a write — the widget self-heals.
 *
 * Bootstrap once from `App.onCreate` via a Hilt entry point so the singleton is created and
 * its subscription starts on cold start.
 */
@Singleton
class WidgetAutoReloader @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val recordsRepository: RecordsRepository,
) {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            combine(
                recordsRepository.observeRecords(searchTerm = null, sinceEpochMillis = 0L),
                recordsRepository.observeQuickPicks(DiaryWidgetScreen.QUICK_PICK_COUNT),
            ) { _, _ -> Unit }
                // Skip the initial "current state" emission so we only react to changes.
                .drop(1)
                // Coalesce bursts of writes (e.g. saving a record cascades to multiple tables).
                .debounce(DEBOUNCE_MS)
                .onEach {
                    val manager = GlanceAppWidgetManager(appContext)
                    if (manager.getGlanceIds(DiaryWidgetScreen::class.java).isNotEmpty()) {
                        Log.i(TAG, "Widget data changed; scheduling diary widget reload")
                        DiaryWidgetScreen.reload(appContext)
                    }
                    if (manager.getGlanceIds(QuickPickWidgetScreen::class.java).isNotEmpty()) {
                        Log.i(TAG, "Widget data changed; scheduling quick pick widget reload")
                        WorkManager.getInstance(appContext)
                            .enqueue(QuickPickReloadWorker.getWorkRequest())
                    }
                }
                .collect {}
        }
    }

    private companion object {
        private const val TAG = "WidgetAutoReloader"
        private const val DEBOUNCE_MS = 250L
    }
}
