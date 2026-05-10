package dev.gaborbiro.dailymacros.features.widget.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.glance.GlanceTheme
import dev.gaborbiro.dailymacros.design.WidgetColorScheme
import dev.gaborbiro.dailymacros.features.common.views.DummyImageStore
import dev.gaborbiro.dailymacros.features.widget.views.LocalImageStoreWidget

@Composable
internal fun WidgetPreviewContext(
    content: @Composable () -> Unit,
) {
    GlanceTheme(colors = WidgetColorScheme.colors()) {
        CompositionLocalProvider(LocalImageStoreWidget provides DummyImageStore) {
            content()
        }
    }
}
