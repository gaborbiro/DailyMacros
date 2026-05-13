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
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.design.WidgetColorScheme
import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelBase
import dev.gaborbiro.dailymacros.features.widget.model.ListUiModelQuickPickFooter
import dev.gaborbiro.dailymacros.features.widget.model.ListUiModelQuickPickHeader
import dev.gaborbiro.dailymacros.features.widget.views.DiaryWidgetView
import dev.gaborbiro.dailymacros.features.widget.views.LocalImageStoreWidget
import dagger.hilt.android.EntryPointAccessors

class DiaryWidgetScreen : GlanceAppWidget() {

    companion object {
        const val PREFS_RECENT_RECORDS = "recent_records"
        const val PREFS_QUICK_PICKS = "quick_pics"
        const val RECORD_DAYS_TO_DISPLAY = 3
        const val QUICK_PICK_COUNT = 15

        fun reload(context: Context) {
            Log.i("DiaryWidgetScreen", "reload()")
            WorkManager.getInstance(context.applicationContext).enqueue(
                ReloadWorker.getReloadWorkRequest(
                    recentRecordsPrefsKey = PREFS_RECENT_RECORDS,
                    quickPicksPrefsKey = PREFS_QUICK_PICKS,
                    recordDaysToDisplay = RECORD_DAYS_TO_DISPLAY,
                    quickPickCount = QUICK_PICK_COUNT,
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

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deps = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetGlanceEntryPoint::class.java,
        ).widgetGlanceDependencies()
        try {
            provideContent {
                GlanceTheme(colors = WidgetColorScheme.colors(context)) {
                    val widgetPrefs = currentState<Preferences>()

                    val state = try {
                        val quickPicks = runCatching {
                            val recordsJSON = widgetPrefs[stringPreferencesKey(PREFS_QUICK_PICKS)]
                            deps.widgetUiMapper.map(
                                PersistenceMapper.deserializeTemplates(recordsJSON)
                            )
                        }.getOrNull() ?: emptyList()

                        val recentRecords = runCatching {
                            val recordsJSON = widgetPrefs[stringPreferencesKey(PREFS_RECENT_RECORDS)]
                            PersistenceMapper.deserializeRecords(recordsJSON)
                                .map(deps.sharedRecordsUiMapper::map)
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
                        WidgetUiState.Success(
                            items = items,
                            imageStore = deps.imageStore,
                            navigator = deps.widgetNavigator,
                        )
                    } catch (t: Throwable) {
                        deps.analyticsLogger.setCustomDataForNextReport("source", "inside provideGlance.provideContent")
                        deps.analyticsLogger.logError(t)
                        WidgetUiState.Error
                    }

                    when (state) {
                        is WidgetUiState.Success -> {
                            CompositionLocalProvider(LocalImageStoreWidget provides state.imageStore) {
                                DiaryWidgetView(
                                    modifier = GlanceModifier.fillMaxSize(),
                                    navigator = state.navigator,
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
            deps.analyticsLogger.setCustomDataForNextReport("source", "outside provideGlance.provideContent")
            deps.analyticsLogger.logError(t)
            provideContent {
                ErrorView()
            }
        }
    }

    private sealed interface WidgetUiState {
        data class Success(
            val items: List<ListUiModelBase>,
            val imageStore: ImageStore,
            val navigator: WidgetNavigator,
        ) : WidgetUiState

        data object Error : WidgetUiState
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
