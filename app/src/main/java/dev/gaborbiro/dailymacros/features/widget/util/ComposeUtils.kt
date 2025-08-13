package dev.gaborbiro.dailymacros.features.widget.util

import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import dev.gaborbiro.dailymacros.design.WidgetColorScheme

@Composable
internal fun WidgetPreview(
    content: @Composable () -> Unit,
) {
    GlanceTheme(colors = WidgetColorScheme.colors()) {
        content()
    }
}
