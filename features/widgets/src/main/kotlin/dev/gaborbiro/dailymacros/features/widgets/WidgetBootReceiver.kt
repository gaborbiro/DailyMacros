package dev.gaborbiro.dailymacros.features.widgets

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.compose
import androidx.work.WorkManager
import dev.gaborbiro.dailymacros.features.widgets.diarywidget.DiaryWidgetScreen
import dev.gaborbiro.dailymacros.features.widgets.quickpickwidget.QuickPickReloadWorker
import dev.gaborbiro.dailymacros.features.widgets.quickpickwidget.QuickPickWidgetScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * After a reboot (or app update) the launcher falls back to the widgets' static initialLayout
 * and nothing re-renders them until the app process happens to start: updatePeriodMillis is 0,
 * so all refreshes are app-driven.
 *
 * This receiver closes that gap. Glance's own update path (GlanceAppWidgetReceiver.onUpdate →
 * session → WorkManager SessionWorker) has proven unreliable right after boot, so instead of
 * going through it, each widget is composed inline via [compose] — which loads the persisted
 * Glance state — and the resulting RemoteViews are pushed straight to [AppWidgetManager].
 * All widget click actions are actionRunCallback/actionStartActivity based, so the snapshot
 * stays fully interactive without a live Glance session.
 *
 * The reload workers are then enqueued to refresh the data behind the immediate render.
 */
class WidgetBootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
                -> {
                Log.i(TAG, "onReceive: ${intent.action}")
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        refreshWidgets(context)
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to refresh widgets after ${intent.action}", t)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private suspend fun refreshWidgets(context: Context) {
        val glanceManager = GlanceAppWidgetManager(context)

        val diaryIds = glanceManager.getGlanceIds(DiaryWidgetScreen::class.java)
        Log.i(TAG, "diary widgets: ${diaryIds.size}")
        renderDirectly(context, glanceManager, DiaryWidgetScreen(), diaryIds)

        val quickPickIds = glanceManager.getGlanceIds(QuickPickWidgetScreen::class.java)
        Log.i(TAG, "quick pick widgets: ${quickPickIds.size}")
        renderDirectly(context, glanceManager, QuickPickWidgetScreen(), quickPickIds)

        // Refresh the underlying data behind the immediate render above.
        if (diaryIds.isNotEmpty()) {
            DiaryWidgetScreen.reload(context)
        }
        if (quickPickIds.isNotEmpty()) {
            WorkManager.getInstance(context.applicationContext)
                .enqueue(QuickPickReloadWorker.getWorkRequest())
        }
    }

    /**
     * Composes each widget from its persisted state and hands the RemoteViews directly to
     * AppWidgetManager, without involving Glance's WorkManager-backed session pipeline.
     */
    @OptIn(ExperimentalGlanceApi::class)
    private suspend fun renderDirectly(
        context: Context,
        glanceManager: GlanceAppWidgetManager,
        widget: GlanceAppWidget,
        glanceIds: List<GlanceId>,
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        glanceIds.forEach { glanceId ->
            val appWidgetId = glanceManager.getAppWidgetId(glanceId)
            try {
                val remoteViews = widget.compose(context = context, id = glanceId)
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                Log.i(TAG, "widget $appWidgetId rendered from persisted state")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to render widget $appWidgetId", t)
            }
        }
    }

    private companion object {
        private const val TAG = "WidgetBootReceiver"
    }
}
