package dev.gaborbiro.dailymacros.features.widgets.quickpick

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.android.EntryPointAccessors
import dev.gaborbiro.dailymacros.design.PaddingWidgetDefault
import dev.gaborbiro.dailymacros.design.WidgetColorScheme
import dev.gaborbiro.dailymacros.features.widgets.PersistenceMapper
import dev.gaborbiro.dailymacros.features.widgets.model.ListUiModelQuickPick
import dev.gaborbiro.dailymacros.features.widgets.views.DescriptionTextStyle
import dev.gaborbiro.dailymacros.features.widgets.views.ListItemImageCornerRadius
import dev.gaborbiro.dailymacros.features.widgets.views.LocalImage
import dev.gaborbiro.dailymacros.features.widgets.views.LocalImageStoreWidget
import dev.gaborbiro.dailymacros.features.widgets.views.TitleTextStyle
import dev.gaborbiro.dailymacros.features.widgets.views.WidgetImageSize

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
                            onTapped = { deps.widgetNavigator.quickPickBodyTapped(uiModel.templateId) }
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
                text = "Long-press to reconfigure",
                style = TextStyle(color = ColorProvider(Color.Gray)),
            )
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        // nothing to clean up
    }
}

@Composable
internal fun QuickPickWidgetView(
    uiModel: ListUiModelQuickPick,
    onTapped: () -> Unit,
) {
    Row(
        modifier = GlanceModifier
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(8.dp)
            .fillMaxWidth()
            .clickable(onTapped),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        uiModel.images.firstOrNull()
            ?.let {
                LocalImage(
                    it,
                    modifier = GlanceModifier
                        .size(WidgetImageSize)
                        .cornerRadius(ListItemImageCornerRadius),
                    contentScale = ContentScale.Crop,
                )
            }
            ?: run {
                Spacer(modifier = GlanceModifier.size(WidgetImageSize))
            }
        Column {
            Text(
                modifier = GlanceModifier
                    .wrapContentHeight()
                    .padding(start = PaddingWidgetDefault, end = PaddingWidgetDefault),
                text = uiModel.title,
                maxLines = 3,
                style = TitleTextStyle,
            )
            uiModel.nutrients?.calories
                ?.let {
                    Text(
                        modifier = GlanceModifier
                            .wrapContentHeight()
                            .padding(start = PaddingWidgetDefault, end = PaddingWidgetDefault),
                        text = it,
                        maxLines = 1,
                        style = DescriptionTextStyle,
                    )
                }
        }
    }
}