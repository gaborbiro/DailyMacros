package dev.gaborbiro.dailymacros.features.widget.workers

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import dev.gaborbiro.dailymacros.repo.records.ApiMapper
import dev.gaborbiro.dailymacros.repo.records.RecordsRepositoryImpl
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template
import dev.gaborbiro.dailymacros.features.widget.NotesWidget
import dev.gaborbiro.dailymacros.data.image.ImageStoreImpl
import dev.gaborbiro.dailymacros.data.db.AppDatabase
import dev.gaborbiro.dailymacros.data.file.FileStoreFactoryImpl
import dev.gaborbiro.dailymacros.util.gson
import java.time.LocalDateTime

internal class ReloadWorkRequest(
    appContext: Context,
    private val workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {

    private val fileStore by lazy { FileStoreFactoryImpl(appContext).getStore("public", keepFiles = true) }
    private val recordsRepository by lazy {
        RecordsRepositoryImpl(
            templatesDAO = AppDatabase.getInstance().templatesDAO(),
            recordsDAO = AppDatabase.getInstance().recordsDAO(),
            mapper = ApiMapper(),
            imageStore = ImageStoreImpl(fileStore),
        )
    }

    companion object {
        private const val RECORD_DAYS_TO_DISPLAY_DEFAULT = 7
        private const val TEMPLATE_COUNT_DEFAULT = 30

        private const val PREFS_RECENT_RECORDS_KEY = "recent_records_key"
        private const val PREFS_TOP_TEMPLATES_KEY = "top_templates_key"
        private const val PREFS_RECORD_DAYS_TO_DISPLAY = "record_days_to_display"
        private const val PREFS_TEMPLATE_COUNT = "template_count"

        fun getWorkRequest(
            recentRecordsPrefsKey: String,
            topTemplatesPrefsKey: String,
            recordDaysToDisplay: Int = RECORD_DAYS_TO_DISPLAY_DEFAULT,
            templateCount: Int = TEMPLATE_COUNT_DEFAULT,
        ): WorkRequest {
            return OneTimeWorkRequestBuilder<ReloadWorkRequest>()
                .setInputData(
                    Data.Builder()
                        .putString(PREFS_RECENT_RECORDS_KEY, recentRecordsPrefsKey)
                        .putString(PREFS_TOP_TEMPLATES_KEY, topTemplatesPrefsKey)
                        .putInt(PREFS_RECORD_DAYS_TO_DISPLAY, recordDaysToDisplay)
                        .putInt(PREFS_TEMPLATE_COUNT, templateCount)
                        .build()
                )
                .build()
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val recordDaysToDisplay =
                workerParameters.inputData.getInt(
                    PREFS_RECORD_DAYS_TO_DISPLAY,
                    RECORD_DAYS_TO_DISPLAY_DEFAULT
                )
            val templateCount =
                workerParameters.inputData.getInt(PREFS_TEMPLATE_COUNT, TEMPLATE_COUNT_DEFAULT)
            val recentRecords =
                recordsRepository.getRecords(LocalDateTime.now().minusDays(recordDaysToDisplay.toLong()))
            val topTemplates = recordsRepository.getTop10().take(templateCount)
            sendToWidgets(applicationContext, recentRecords, topTemplates)
            Result.success()
        } catch (t: Throwable) {
            t.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun sendToWidgets(
        context: Context,
        recentRecords: List<Record>,
        topTemplates: List<Template>,
    ) {
        val recentRecordsPrefsKey =
            stringPreferencesKey(workerParameters.inputData.getString(PREFS_RECENT_RECORDS_KEY)!!)
        val topTemplatesPrefsKey =
            stringPreferencesKey(workerParameters.inputData.getString(PREFS_TOP_TEMPLATES_KEY)!!)

        val glanceIds = GlanceAppWidgetManager(context)
            .getGlanceIds(NotesWidget::class.java)
        val recordsJson = gson.toJson(recentRecords)
        val templatesJson = gson.toJson(topTemplates)
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[recentRecordsPrefsKey] = recordsJson
                prefs[topTemplatesPrefsKey] = templatesJson
            }
        }
        NotesWidget().updateAll(context)
    }
}

//        suspend fun startUpdate(context: Context) {
//            val widgetCount =
//                GlanceAppWidgetManager(context).getGlanceIds(NotesWidget::class.java).size
//            println("NotesWidget startUpdate $widgetCount")
//            if (widgetCount > 0) {
//                val request = PeriodicWorkRequest
//                    .Builder(NotesWidgetsUpdater::class.java, 15, TimeUnit.MINUTES)
//                    .build()
//
//                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
//                    UNIQUE_WORK_NAME,
//                    ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
//                    request,
//                )
//            }
//        }
