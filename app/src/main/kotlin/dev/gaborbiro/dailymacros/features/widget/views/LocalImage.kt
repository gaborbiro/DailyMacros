package dev.gaborbiro.dailymacros.features.widget.views

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

val LocalImageStoreWidget = staticCompositionLocalOf<ImageStore> {
    error("LocalImageStoreWidget not provided")
}

@Composable
fun LocalImage(
    name: String,
    modifier: GlanceModifier = GlanceModifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit = { Box(modifier.background(Color.LightGray.copy(alpha = 0.15f))) {} },
    error: @Composable () -> Unit = { Box(modifier.background(Color.Red.copy(alpha = 0.08f))) {} },
) {
    val store = LocalImageStoreWidget.current
    var bmp by remember(name, store) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(name, store) { mutableStateOf(false) }

    LaunchedEffect(name, store) {
        failed = false
        bmp = null
        try {
            bmp = store.read(name, thumbnail = true)
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
