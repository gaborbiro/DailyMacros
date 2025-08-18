package dev.gaborbiro.dailymacros.features.common.view

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore

val LocalImageStore = staticCompositionLocalOf<ImageStore> {
    error("LocalImageStore not provided")
}

@Composable
fun LocalImage(
    name: String,
    modifier: Modifier = Modifier,
    thumbnail: Boolean = true,
    contentDescription: String,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit = { Box(modifier.background(Color.LightGray.copy(alpha = 0.15f))) },
    error: @Composable () -> Unit = { Box(modifier.background(Color.Red.copy(alpha = 0.08f))) },
) {
    val store = LocalImageStore.current
    var bmp by remember(name, thumbnail, store) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(name, thumbnail, store) { mutableStateOf(false) }

    LaunchedEffect(name, thumbnail, store) {
        failed = false
        bmp = null
        try {
            bmp = store.read(name, thumbnail)
            if (bmp == null) failed = true
        } catch (_: Throwable) {
            failed = true
        }
    }

    when {
        bmp != null -> Image(
            bitmap = bmp!!.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )

        failed -> error()
        else -> placeholder()
    }
}
