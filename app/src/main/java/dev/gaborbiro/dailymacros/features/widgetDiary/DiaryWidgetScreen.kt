package dev.gaborbiro.dailymacros.features.widgetDiary

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.work.WorkManager
import com.google.gson.reflect.TypeToken
import dev.gaborbiro.dailymacros.AnalyticsLogger
import dev.gaborbiro.dailymacros.App
import dev.gaborbiro.dailymacros.data.file.FileStoreFactoryImpl
import dev.gaborbiro.dailymacros.data.image.ImageStoreImpl
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.design.WidgetColorScheme
import dev.gaborbiro.dailymacros.features.common.DateUIMapper
import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.common.RecordsUIMapper
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelBase
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelQuickPickFooter
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelQuickPickHeader
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelRecord
import dev.gaborbiro.dailymacros.features.widgetDiary.views.LocalImageStoreWidget
import dev.gaborbiro.dailymacros.features.widgetDiary.views.DiaryWidgetView
import dev.gaborbiro.dailymacros.features.widgetDiary.workers.ReloadWorker
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template
import dev.gaborbiro.dailymacros.util.gson

class DiaryWidgetScreen : GlanceAppWidget() {

    companion object {
        const val PREFS_RECENT_RECORDS = "recent_records"
        const val PREFS_QUICK_PICKS = "quick_pics"

        fun reload() {
            Log.i("DiaryWidgetScreen", "reload()")
            WorkManager.getInstance(App.appContext).enqueue(
                ReloadWorker.getWorkRequest(
                    recentRecordsPrefsKey = PREFS_RECENT_RECORDS,
                    quickPicksPrefsKey = PREFS_QUICK_PICKS,
                    recordDaysToDisplay = 3,
                    quickPickCount = 15,
                )
            )
        }

        suspend fun cleanup(context: Context) {
            val widgetCount = GlanceAppWidgetManager(context)
                .getGlanceIds(DiaryWidgetScreen::class.java)
                .size
            if (widgetCount == 0) {
                WorkManager.getInstance(context).cancelAllWork()
            }
        }
    }

    override val stateDefinition = WidgetPreferences
    private val analyticsLogger = AnalyticsLogger()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            provideContent {
                GlanceTheme(colors = WidgetColorScheme.colors(context)) {
                    val widgetPrefs = currentState<Preferences>()

                    val state = try {
                        val fileStore =
                            FileStoreFactoryImpl(context).getStore("public", keepFiles = true)
                        val imageStore: ImageStore = ImageStoreImpl(fileStore)
                        val dateUIMapper = DateUIMapper()
                        val macrosUIMapper = MacrosUIMapper(dateUIMapper)
                        val recordsUIMapper = RecordsUIMapper(macrosUIMapper, dateUIMapper)
                        val widgetUIMapper = WidgetUIMapper(macrosUIMapper)

                        val topTemplates = widgetUIMapper.map(widgetPrefs.retrieveTopTemplates())
                        val recentRecords = recordsUIMapper
                            .map(widgetPrefs.retrieveRecentRecords(), showDay = true)
                            .filterIsInstance<ListUIModelRecord>()

                        val items = buildList {
                            if (recentRecords.isNotEmpty() || topTemplates.isNotEmpty()) {
                                addAll(recentRecords.take(3))
                                if (topTemplates.isNotEmpty()) {
                                    add(ListUIModelQuickPickHeader)
                                    addAll(topTemplates)
                                    add(ListUIModelQuickPickFooter)
                                }
                                addAll(recentRecords.drop(3))
                            }
                        }
                        WidgetUiState.Success(items, imageStore)
                    } catch (t: Throwable) {
                        analyticsLogger.setCustomDataForNextReport("source", "inside provideGlance.provideContent")
                        analyticsLogger.logError(t)
                        WidgetUiState.Error
                    }

                    when (state) {
                        is WidgetUiState.Success -> {
                            CompositionLocalProvider(LocalImageStoreWidget provides state.imageStore) {
                                DiaryWidgetView(
                                    modifier = GlanceModifier.fillMaxSize(),
                                    actionProvider = WidgetActionProviderImpl(),
                                    items = state.items
                                )
                            }
                        }

                        WidgetUiState.Error -> {
                            ErrorView()
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            analyticsLogger.setCustomDataForNextReport("source", "outside provideGlance.provideContent")
            analyticsLogger.logError(t)
            provideContent {
                ErrorView()
            }
        }
    }

    sealed interface WidgetUiState {
        data class Success(val items: List<ListUIModelBase>, val imageStore: ImageStore) :
            WidgetUiState

        object Error : WidgetUiState
    }

    @Composable
    private fun ErrorView() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Widget error",
                style = TextStyle(color = ColorProvider(Color.Red))
            )
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        cleanup(context)
    }
}

private fun Preferences.retrieveRecentRecords(): List<Record> {
    val recordsJSON = this[stringPreferencesKey(DiaryWidgetScreen.PREFS_RECENT_RECORDS)]
    return recordsJSON
        ?.let {
            val itemType = object : TypeToken<List<Record>>() {}.type
            gson.fromJson(recordsJSON, itemType)
        }
        ?: emptyList()
}

private fun Preferences.retrieveTopTemplates(): List<Template> {
    val recordsJSON = this[stringPreferencesKey(DiaryWidgetScreen.PREFS_QUICK_PICKS)]
    return recordsJSON
        ?.let {
            val itemType = object : TypeToken<List<Template>>() {}.type
            gson.fromJson(recordsJSON, itemType)
        }
        ?: emptyList()
}
