package dev.gaborbiro.dailymacros.features.widgetDiary.util

import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import dev.gaborbiro.dailymacros.design.WidgetColorScheme
import dev.gaborbiro.dailymacros.features.widgetDiary.views.PreviewImageStoreProviderWidget

@Composable
internal fun WidgetPreview(
    content: @Composable () -> Unit,
) {
    GlanceTheme(colors = WidgetColorScheme.colors()) {
        PreviewImageStoreProviderWidget {
            content()
        }
    }
}
