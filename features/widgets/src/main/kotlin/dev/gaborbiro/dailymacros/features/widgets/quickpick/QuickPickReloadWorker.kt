package dev.gaborbiro.dailymacros.features.widgets.quickpick

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.features.widgets.PersistenceMapper
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository

@HiltWorker
class QuickPickReloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val recordsRepository: RecordsRepository,
    private val analyticsLogger: AnalyticsLogger,
) : CoroutineWorker(appContext, params) {

    companion object {
        fun getWorkRequest(): WorkRequest = OneTimeWorkRequestBuilder<QuickPickReloadWorker>().build()
    }

    override suspend fun doWork(): Result {
        return try {
            val manager = GlanceAppWidgetManager(applicationContext)
            val glanceIds = manager.getGlanceIds(QuickPickWidgetScreen::class.java)

            glanceIds.forEach { glanceId ->
                val appWidgetId = manager.getAppWidgetId(glanceId)
                val prefs = QuickPickWidgetScreen()
                    .getAppWidgetState<Preferences>(applicationContext, glanceId)
                val templateId = prefs[QuickPickWidgetScreen.templateIdKey(appWidgetId)]
                if (templateId != null) {
                    val template = recordsRepository.getTemplate(templateId)
                    val json = PersistenceMapper.serializeTemplates(listOf(template))
                    updateAppWidgetState(applicationContext, glanceId) { widgetPrefs ->
                        widgetPrefs[QuickPickWidgetScreen.templateJsonKey(appWidgetId)] = json
                    }
                }
            }
            QuickPickWidgetScreen().updateAll(applicationContext)
            Result.success()
        } catch (t: Throwable) {
            analyticsLogger.logError(t)
            Result.failure()
        }
    }
}
