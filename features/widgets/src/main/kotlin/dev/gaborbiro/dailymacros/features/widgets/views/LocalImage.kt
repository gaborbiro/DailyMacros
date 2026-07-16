package dev.gaborbiro.dailymacros.features.widgets.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import kotlinx.coroutines.runBlocking

val LocalImageStoreWidget = staticCompositionLocalOf<ImageStore> {
    error("LocalImageStoreWidget not provided")
}

val LocalWidgetIsPreview = compositionLocalOf { false }

@Composable
fun LocalImage(
    imageFilename: String,
    modifier: GlanceModifier = GlanceModifier,
    contentScale: ContentScale = ContentScale.Crop,
    thumbnail: Boolean = true,
    placeholder: @Composable () -> Unit = { Box(modifier.background(Color.LightGray.copy(alpha = 0.15f))) {} },
    error: @Composable () -> Unit = { Box(modifier.background(Color.Red.copy(alpha = 0.08f))) {} },
) {
    val store = LocalImageStoreWidget.current
    val isPreview = LocalWidgetIsPreview.current
    var bmp by remember(imageFilename, store, thumbnail) {
        mutableStateOf(
            if (isPreview) runCatching { runBlocking { store.read(imageFilename, thumbnail) } }.getOrNull()
            else null
        )
    }
    var failed by remember(imageFilename, store, thumbnail) { mutableStateOf(false) }

    LaunchedEffect(imageFilename, store, thumbnail) {
        if (bmp != null) return@LaunchedEffect
        failed = false
        try {
            bmp = store.read(filename = imageFilename, thumbnail = thumbnail)
            if (bmp == null) failed = true
        } catch (_: Throwable) {
            failed = true
        }
    }

    when {
        bmp != null -> Image(
            modifier = modifier,
            contentScale = contentScale,
            contentDescription = null,
            provider = ImageProvider(bmp!!)
        )

        failed -> error()
        else -> placeholder()
    }
}
