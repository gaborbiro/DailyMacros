package dev.gaborbiro.nutri.features.widget

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.gaborbiro.nutri.data.records.domain.model.Record
import dev.gaborbiro.nutri.data.records.domain.model.Template
import dev.gaborbiro.nutri.design.NotesGlanceColorScheme
import dev.gaborbiro.nutri.features.common.RecordsUIMapper
import dev.gaborbiro.nutri.features.common.TemplatesUIMapper
import dev.gaborbiro.nutri.features.widget.views.NotesWidgetContent
import dev.gaborbiro.nutri.features.widget.workers.ReloadWorkRequest
import dev.gaborbiro.nutri.store.bitmap.BitmapStore
import dev.gaborbiro.nutri.store.file.FileStoreFactoryImpl
import dev.gaborbiro.nutri.util.gson

class NotesWidget : GlanceAppWidget() {

    companion object {
        const val PREFS_RECENT_RECORDS = "recent_records"
        const val PREFS_TOP_TEMPLATES = "top_templates"

        fun reload(context: Context) {
            WorkManager.getInstance(context).enqueue(
                ReloadWorkRequest.getWorkRequest(
                    recentRecordsPrefsKey = PREFS_RECENT_RECORDS,
                    topTemplatesPrefsKey = PREFS_TOP_TEMPLATES,
                    recordDaysToDisplay = 30,
                    templateCount = 30,
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
            val fileStore = FileStoreFactoryImpl(LocalContext.current).getStore("public", keepFiles = true)
            val bitmapStore = BitmapStore(fileStore)
            val recordsUIMapper = RecordsUIMapper(bitmapStore)
            val recentRecords = recordsUIMapper.map(
                records = prefs.retrieveRecentRecords(),
                thumbnail = true,
            )
            val templatesUIMapper = TemplatesUIMapper(bitmapStore)
            val topTemplates = templatesUIMapper.map(
                records = prefs.retrieveTopTemplates(),
                thumbnail = true,
            )

            var showTopTemplates by remember { mutableStateOf(false) }

            GlanceTheme(colors = NotesGlanceColorScheme.colors) {
                NotesWidgetContent(
                    modifier = GlanceModifier
                        .fillMaxSize(),
                    navigator = NotesWidgetNavigatorImpl(),
                    showTopTemplates = showTopTemplates,
                    onTemplatesExpandButtonTapped = {
                        showTopTemplates = showTopTemplates.not()
                    },
                    recentRecords = recentRecords,
                    topTemplates = topTemplates,
                )
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

class NotesWidgetReceiverSmall : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = NotesWidget()
}
