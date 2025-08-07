package dev.gaborbiro.dailymacros.features.widget.workers

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import dev.gaborbiro.dailymacros.features.widget.NotesWidget

class MarkAsOutdatedWorkRequest(
    appContext: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    companion object {
        private const val PREFS_OUTDATED_KEY = "outdated_key"

        fun getWorkRequest(
            outdatedPrefsKey: String,
        ): WorkRequest {
            return OneTimeWorkRequestBuilder<MarkAsOutdatedWorkRequest>()
                .setInputData(
                    Data.Builder()
                        .putString(PREFS_OUTDATED_KEY, outdatedPrefsKey)
                        .build()
                )
                .build()
        }
    }

    override suspend fun doWork(): Result {
        return try {
            sendToWidgets(applicationContext)
            Result.success()
        } catch (t: Throwable) {
            t.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun sendToWidgets(
        context: Context,
    ) {
        val outdatedPrefsKey =
            booleanPreferencesKey(workerParameters.inputData.getString(PREFS_OUTDATED_KEY)!!)

        val glanceIds = GlanceAppWidgetManager(context)
            .getGlanceIds(NotesWidget::class.java)
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[outdatedPrefsKey] = true
            }
        }
        NotesWidget().updateAll(context)
    }
}
