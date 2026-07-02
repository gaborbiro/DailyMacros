package dev.gaborbiro.dailymacros.features.widgets.quickpickwidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
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
import dagger.hilt.android.EntryPointAccessors
import dev.gaborbiro.dailymacros.design.WidgetColorScheme
import dev.gaborbiro.dailymacros.features.widgets.PersistenceMapper
import dev.gaborbiro.dailymacros.features.widgets.views.LocalImageStoreWidget

class QuickPickWidgetScreen : GlanceAppWidget() {

    companion object {
        fun templateIdKey(appWidgetId: Int) = longPreferencesKey("template_id_$appWidgetId")
        fun templateJsonKey(appWidgetId: Int) = stringPreferencesKey("template_json_$appWidgetId")
    }

    override val stateDefinition = QuickPickWidgetPreferences

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deps = EntryPointAccessors.fromApplication(
            context.applicationContext,
            QuickPickGlanceEntryPoint::class.java,
        ).quickPickGlanceDependencies()

        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        provideContent {
            GlanceTheme(colors = WidgetColorScheme.colors(context)) {
                val prefs = currentState<Preferences>()
                val templateJson = prefs[templateJsonKey(appWidgetId)]
                val uiModel = templateJson?.let { json ->
                    runCatching {
                        PersistenceMapper.deserializeTemplates(json).firstOrNull()
                            ?.let { deps.widgetUiMapper.map(listOf(it)).firstOrNull() }
                    }.getOrNull()
                }

                if (uiModel != null) {
                    CompositionLocalProvider(LocalImageStoreWidget provides deps.imageStore) {
                        QuickPickWidgetView(
                            uiModel = uiModel,
                            onTapped = deps.widgetNavigator.quickPickWidgetTapped(uiModel.templateId),
                        )
                    }
                } else {
                    NotConfiguredView()
                }
            }
        }
    }

    @Composable
    private fun NotConfiguredView() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Remove and re-add to reconfigure",
                style = TextStyle(color = ColorProvider(Color.Gray)),
            )
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        // nothing to clean up
    }
}
