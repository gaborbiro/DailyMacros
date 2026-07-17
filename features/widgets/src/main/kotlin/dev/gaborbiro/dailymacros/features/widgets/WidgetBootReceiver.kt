package dev.gaborbiro.dailymacros.features.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
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
 * so all refreshes are app-driven. This receiver closes that gap by re-rendering both widgets
 * from their persisted Glance state right away, then enqueueing the reload workers to freshen
 * the data itself.
 */
class WidgetBootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
                -> {
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        val manager = GlanceAppWidgetManager(context)
                        if (manager.getGlanceIds(DiaryWidgetScreen::class.java).isNotEmpty()) {
                            DiaryWidgetScreen().updateAll(context)
                            DiaryWidgetScreen.reload(context)
                        }
                        if (manager.getGlanceIds(QuickPickWidgetScreen::class.java).isNotEmpty()) {
                            QuickPickWidgetScreen().updateAll(context)
                            WorkManager.getInstance(context.applicationContext)
                                .enqueue(QuickPickReloadWorker.getWorkRequest())
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to refresh widgets after ${intent.action}", t)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private companion object {
        private const val TAG = "WidgetBootReceiver"
    }
}
