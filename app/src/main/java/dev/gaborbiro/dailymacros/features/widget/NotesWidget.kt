package dev.gaborbiro.dailymacros.features.widget

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
import dev.gaborbiro.dailymacros.data.records.domain.model.Record
import dev.gaborbiro.dailymacros.data.records.domain.model.Template
import dev.gaborbiro.dailymacros.design.WidgetColorScheme
import dev.gaborbiro.dailymacros.features.common.NutrientsUIMapper
import dev.gaborbiro.dailymacros.features.common.RecordsUIMapper
import dev.gaborbiro.dailymacros.features.widget.views.WidgetContent
import dev.gaborbiro.dailymacros.features.widget.workers.ReloadWorkRequest
import dev.gaborbiro.dailymacros.store.bitmap.BitmapStore
import dev.gaborbiro.dailymacros.store.file.FileStoreFactoryImpl
import dev.gaborbiro.dailymacros.util.gson

class NotesWidget : GlanceAppWidget() {

    companion object {
        const val PREFS_RECENT_RECORDS = "recent_records"
        const val PREFS_TOP_TEMPLATES = "top_templates"

        fun reload(context: Context) {
            WorkManager.getInstance(context).enqueue(
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
            val bitmapStore = BitmapStore(fileStore)
            val nutrientsUIMapper = NutrientsUIMapper()
            val recordsUIMapper = RecordsUIMapper(bitmapStore, nutrientsUIMapper)
            val recentRecords = recordsUIMapper.map(
                records = prefs.retrieveRecentRecords(),
                thumbnail = true,
            )
            val widgetUIMapper = WidgetUIMapper(bitmapStore, nutrientsUIMapper)
            val topTemplates = widgetUIMapper.map(
                templates = prefs.retrieveTopTemplates(),
                thumbnail = true,
            )

            var showTopTemplates by remember { mutableStateOf(false) }

            GlanceTheme(colors = WidgetColorScheme.colors(context)) {
                WidgetContent(
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
