package dev.gaborbiro.dailymacros.features.widgets

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.features.widgets.diarywidget.DiaryWidgetScreen
import dev.gaborbiro.dailymacros.features.widgets.quickpickwidget.QuickPickWidgetScreen
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subscribes to the records repository for the lifetime of the app process and triggers a
 * widget reload whenever underlying data changes. With this in place, no caller needs to
 * explicitly schedule widget reloads after a write — the widget self-heals.
 *
 * Bootstrap once from `App.onCreate` via a Hilt entry point so the singleton is created and
 * its subscription starts on cold start.
 *
 * The reload is done inline in this coroutine scope rather than via WorkManager to avoid
 * the scheduler's 1-5 second startup overhead.
 */
@Singleton
class WidgetAutoReloader @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val recordsRepository: RecordsRepository,
) {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    @OptIn(FlowPreview::class)
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
                .onEach { reloadWidgets() }
                .collect {}
        }
    }

    private suspend fun reloadWidgets() {
        val manager = GlanceAppWidgetManager(appContext)

        val diaryIds = manager.getGlanceIds(DiaryWidgetScreen::class.java)
        if (diaryIds.isNotEmpty()) {
            Log.i(TAG, "Widget data changed; reloading diary widget")
            val since = ZonedDateTime.now()
                .minusDays(DiaryWidgetScreen.RECORD_DAYS_TO_DISPLAY.toLong())
            val records = recordsRepository.getRecords(since)
            val quickPicks = recordsRepository.getQuickPicks(DiaryWidgetScreen.QUICK_PICK_COUNT)
            val recordsJson = PersistenceMapper.serializeRecords(records)
            val quickPicksJson = PersistenceMapper.serializeTemplates(quickPicks)
            val recordsKey = stringPreferencesKey(DiaryWidgetScreen.PREFS_RECENT_RECORDS)
            val quickPicksKey = stringPreferencesKey(DiaryWidgetScreen.PREFS_QUICK_PICKS)
            diaryIds.forEach { glanceId ->
                updateAppWidgetState(appContext, glanceId) { prefs ->
                    prefs[recordsKey] = recordsJson
                    prefs[quickPicksKey] = quickPicksJson
                }
            }
            DiaryWidgetScreen().updateAll(appContext)
        }

        val quickPickIds = manager.getGlanceIds(QuickPickWidgetScreen::class.java)
        if (quickPickIds.isNotEmpty()) {
            Log.i(TAG, "Widget data changed; reloading quick pick widget")
            quickPickIds.forEach { glanceId ->
                val appWidgetId = manager.getAppWidgetId(glanceId)
                val prefs = QuickPickWidgetScreen()
                    .getAppWidgetState<Preferences>(appContext, glanceId)
                val templateId = prefs[QuickPickWidgetScreen.templateIdKey(appWidgetId)]
                if (templateId != null) {
                    val template = recordsRepository.getTemplate(templateId)
                    val json = PersistenceMapper.serializeTemplates(listOf(template))
                    updateAppWidgetState(appContext, glanceId) { widgetPrefs ->
                        widgetPrefs[QuickPickWidgetScreen.templateJsonKey(appWidgetId)] = json
                    }
                }
            }
            QuickPickWidgetScreen().updateAll(appContext)
        }
    }

    private companion object {
        private const val TAG = "WidgetAutoReloader"
        private const val DEBOUNCE_MS = 250L
    }
}
