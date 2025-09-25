package dev.gaborbiro.dailymacros.features.widgetDiary.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import dev.gaborbiro.dailymacros.features.common.views.PreviewImageStore

@Composable
fun PreviewImageStoreProviderWidget(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalImageStoreWidget provides PreviewImageStore) {
        content()
    }
}
