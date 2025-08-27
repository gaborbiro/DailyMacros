package dev.gaborbiro.dailymacros.features.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.CompositionLocalProvider
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.fillMaxSize
import androidx.work.WorkManager
import com.google.gson.reflect.TypeToken
import dev.gaborbiro.dailymacros.App
import dev.gaborbiro.dailymacros.data.file.FileStoreFactoryImpl
import dev.gaborbiro.dailymacros.data.image.ImageStoreImpl
import dev.gaborbiro.dailymacros.design.WidgetColorScheme
import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.common.RecordsUIMapper
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.features.common.model.TemplatesEndUIModel
import dev.gaborbiro.dailymacros.features.common.model.TemplatesStartUIModel
import dev.gaborbiro.dailymacros.features.widget.views.LocalImageStore
import dev.gaborbiro.dailymacros.features.widget.views.WidgetContent
import dev.gaborbiro.dailymacros.features.widget.workers.ReloadWorkRequest
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template
import dev.gaborbiro.dailymacros.util.gson

class NotesWidget : GlanceAppWidget() {

    companion object {
        const val PREFS_RECENT_RECORDS = "recent_records"
        const val PREFS_TOP_TEMPLATES = "top_templates"

        fun reload() {
            Log.i("NotesWidget", "reload()")
            WorkManager.getInstance(App.appContext).enqueue(
                ReloadWorkRequest.getWorkRequest(
                    recentRecordsPrefsKey = PREFS_RECENT_RECORDS,
                    topTemplatesPrefsKey = PREFS_TOP_TEMPLATES,
                    recordDaysToDisplay = 3,
                    templateCount = 15,
                )
            )
        }

        suspend fun cleanup(context: Context) {
            val widgetCount = GlanceAppWidgetManager(context)
                .getGlanceIds(NotesWidget::class.java)
                .size
            if (widgetCount == 0) {
                WorkManager.getInstance(context).cancelAllWork()
            }
        }
    }

    override val stateDefinition = NotesWidgetPreferences

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val fileStore =
                FileStoreFactoryImpl(LocalContext.current).getStore("public", keepFiles = true)
            val imageStore = ImageStoreImpl(fileStore)
            val macrosUIMapper = MacrosUIMapper()
            val recordsUIMapper = RecordsUIMapper(macrosUIMapper)
            val widgetUIMapper = WidgetUIMapper(macrosUIMapper)
            val topTemplates = widgetUIMapper.map(templates = prefs.retrieveTopTemplates())
            val recentRecords = recordsUIMapper.map(records = prefs.retrieveRecentRecords())
                .filterIsInstance<RecordUIModel>()
            val items = buildList {
                if (recentRecords.isNotEmpty() || topTemplates.isNotEmpty()) {
                    addAll(recentRecords.take(3))
                    if (topTemplates.isNotEmpty()) {
                        add(TemplatesStartUIModel())
                        addAll(topTemplates)
                        add(TemplatesEndUIModel())
                    }
                    addAll(recentRecords.drop(3))
                }
            }

            GlanceTheme(colors = WidgetColorScheme.colors(context)) {
                CompositionLocalProvider(LocalImageStore provides imageStore) {
                    WidgetContent(
                        modifier = GlanceModifier
                            .fillMaxSize(),
                        navigator = NotesWidgetNavigatorImpl(),
                        items = items.toList(),
                    )
                }
            }
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        cleanup(context)
    }
}

fun Preferences.retrieveRecentRecords(): List<Record> {
    val recordsJSON = this[stringPreferencesKey(NotesWidget.PREFS_RECENT_RECORDS)]
    return recordsJSON
        ?.let {
            val itemType = object : TypeToken<List<Record>>() {}.type
            gson.fromJson(recordsJSON, itemType)
        }
        ?: emptyList()
}

fun Preferences.retrieveTopTemplates(): List<Template> {
    val recordsJSON = this[stringPreferencesKey(NotesWidget.PREFS_TOP_TEMPLATES)]
    return recordsJSON
        ?.let {
            val itemType = object : TypeToken<List<Template>>() {}.type
            gson.fromJson(recordsJSON, itemType)
        }
        ?: emptyList()
}

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = NotesWidget()
}
