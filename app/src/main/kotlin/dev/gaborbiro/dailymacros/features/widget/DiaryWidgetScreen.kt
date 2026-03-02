package dev.gaborbiro.dailymacros.features.widget

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
import dev.gaborbiro.dailymacros.App
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.file.FileStoreFactoryImpl
import dev.gaborbiro.dailymacros.data.image.ImageStoreImpl
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.design.WidgetColorScheme
import dev.gaborbiro.dailymacros.features.common.NutrientsUiMapper
import dev.gaborbiro.dailymacros.features.common.SharedRecordsUiMapper
import dev.gaborbiro.dailymacros.features.common.model.ListUiModelBase
import dev.gaborbiro.dailymacros.features.common.model.ListUiModelQuickPickFooter
import dev.gaborbiro.dailymacros.features.common.model.ListUiModelQuickPickHeader
import dev.gaborbiro.dailymacros.features.widget.views.DiaryWidgetView
import dev.gaborbiro.dailymacros.features.widget.views.LocalImageStoreWidget

class DiaryWidgetScreen : GlanceAppWidget() {

    companion object {
        const val PREFS_RECENT_RECORDS = "recent_records"
        const val PREFS_QUICK_PICKS = "quick_pics"

        fun reload() {
            Log.i("DiaryWidgetScreen", "reload()")
            WorkManager.getInstance(App.appContext).enqueue(
                ReloadWorker.getReloadWorkRequest(
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
                        val nutrientsUiMapper = NutrientsUiMapper()
                        val dateUiMapper = DateUiMapper()
                        val recordsUiMapper = SharedRecordsUiMapper(nutrientsUiMapper, dateUiMapper)
                        val widgetUiMapper = WidgetUiMapper(nutrientsUiMapper)

                        val quickPicks = runCatching {
                            val recordsJSON = widgetPrefs[stringPreferencesKey(PREFS_QUICK_PICKS)]
                            widgetUiMapper.map(
                                PersistenceMapper.deserializeTemplates(recordsJSON)
                            )
                        }.getOrNull() ?: emptyList()

                        val recentRecords = runCatching {
                            val recordsJSON = widgetPrefs[stringPreferencesKey(PREFS_RECENT_RECORDS)]
                            PersistenceMapper.deserializeRecords(recordsJSON)
                                .map(recordsUiMapper::map)
                        }.getOrNull() ?: emptyList()

                        val items = buildList {
                            if (recentRecords.isNotEmpty() || quickPicks.isNotEmpty()) {
                                addAll(recentRecords.take(3))
                                if (quickPicks.isNotEmpty()) {
                                    add(ListUiModelQuickPickHeader)
                                    addAll(quickPicks)
                                    add(ListUiModelQuickPickFooter)
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

    private sealed interface WidgetUiState {
        data class Success(val items: List<ListUiModelBase>, val imageStore: ImageStore) :
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
