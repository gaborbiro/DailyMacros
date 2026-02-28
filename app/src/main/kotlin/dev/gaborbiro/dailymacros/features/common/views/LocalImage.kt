package dev.gaborbiro.dailymacros.features.common.views

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore

val LocalImageStore = staticCompositionLocalOf<ImageStore> {
    error("LocalImageStore not provided")
}

@Composable
fun LocalImage(
    name: String,
    modifier: Modifier = Modifier,
    contentDescription: String,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit = { Box(modifier.background(Color.LightGray.copy(alpha = 0.15f))) },
    error: @Composable () -> Unit = { Box(modifier.background(Color.Red.copy(alpha = 0.08f))) },
) {
    val store = LocalImageStore.current

    val bmp by produceState<Bitmap?>(initialValue = null, key1 = name, key2 = store) {
        value = null
        value = try {
            store.read(name, thumbnail = true)
        } catch (_: Throwable) {
            null
        }
    }
    val imageBitmap = remember(bmp) { bmp?.asImageBitmap() }

    when {
        imageBitmap != null -> Image(
            painter = BitmapPainter(imageBitmap, filterQuality = FilterQuality.Low),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )

        bmp == null -> placeholder()
        else -> error()
    }
}
