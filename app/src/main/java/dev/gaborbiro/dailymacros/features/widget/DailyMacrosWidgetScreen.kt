package dev.gaborbiro.dailymacros.features.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.CompositionLocalProvider
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
import dev.gaborbiro.dailymacros.features.common.AppPrefs
import dev.gaborbiro.dailymacros.features.common.DateUIMapper
import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.common.RecordsUIMapper
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelRecord
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelTop10SectionEnd
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelTop10SectionStart
import dev.gaborbiro.dailymacros.features.widget.views.LocalImageStore
import dev.gaborbiro.dailymacros.features.widget.views.WidgetView
import dev.gaborbiro.dailymacros.features.widget.workers.ReloadWorkRequest
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template
import dev.gaborbiro.dailymacros.util.gson

class DailyMacrosWidgetScreen : GlanceAppWidget() {

    companion object {
        const val PREFS_RECENT_RECORDS = "recent_records"
        const val PREFS_TOP_TEMPLATES = "top_templates"
        val PREFS_SHOW_QUICK_ADD_TOOLTIP = booleanPreferencesKey("show_quick_add_tooltip")

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
                .getGlanceIds(DailyMacrosWidgetScreen::class.java)
                .size
            if (widgetCount == 0) {
                WorkManager.getInstance(context).cancelAllWork()
            }
        }
    }

    override val stateDefinition = WidgetPreferences

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appPrefs = AppPrefs(context)
        provideContent {
            val widgetPrefs = currentState<Preferences>()
            val fileStore =
                FileStoreFactoryImpl(LocalContext.current).getStore("public", keepFiles = true)
            val imageStore = ImageStoreImpl(fileStore)
            val dateUIMapper = DateUIMapper()
            val macrosUIMapper = MacrosUIMapper(dateUIMapper)
            val recordsUIMapper = RecordsUIMapper(macrosUIMapper, dateUIMapper)
            val widgetUIMapper = WidgetUIMapper(macrosUIMapper)
            val topTemplates = widgetUIMapper.map(templates = widgetPrefs.retrieveTopTemplates())
            val recentRecords =
                recordsUIMapper.map(records = widgetPrefs.retrieveRecentRecords(), showDay = true)
                    .filterIsInstance<ListUIModelRecord>()
            val items = buildList {
                if (recentRecords.isNotEmpty() || topTemplates.isNotEmpty()) {
                    addAll(recentRecords.take(3))
                    if (topTemplates.isNotEmpty()) {
                        add(ListUIModelTop10SectionStart(widgetPrefs.showQuickAddTooltip(default = appPrefs.showTooltipQuickAdd)))
                        addAll(topTemplates)
                        add(ListUIModelTop10SectionEnd())
                    }
                    addAll(recentRecords.drop(3))
                }
            }
            println("recompose")
            GlanceTheme(colors = WidgetColorScheme.colors(context)) {
                CompositionLocalProvider(LocalImageStore provides imageStore) {
                    WidgetView(
                        modifier = GlanceModifier
                            .fillMaxSize(),
                        actionProvider = WidgetActionProviderImpl(),
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
    val recordsJSON = this[stringPreferencesKey(DailyMacrosWidgetScreen.PREFS_RECENT_RECORDS)]
    return recordsJSON
        ?.let {
            val itemType = object : TypeToken<List<Record>>() {}.type
            gson.fromJson(recordsJSON, itemType)
        }
        ?: emptyList()
}

fun Preferences.retrieveTopTemplates(): List<Template> {
    val recordsJSON = this[stringPreferencesKey(DailyMacrosWidgetScreen.PREFS_TOP_TEMPLATES)]
    return recordsJSON
        ?.let {
            val itemType = object : TypeToken<List<Template>>() {}.type
            gson.fromJson(recordsJSON, itemType)
        }
        ?: emptyList()
}

fun Preferences.showQuickAddTooltip(default: Boolean): Boolean =
    this[DailyMacrosWidgetScreen.PREFS_SHOW_QUICK_ADD_TOOLTIP] ?: default

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = DailyMacrosWidgetScreen()
}
