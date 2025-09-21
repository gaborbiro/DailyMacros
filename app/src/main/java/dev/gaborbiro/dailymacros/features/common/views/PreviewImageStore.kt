package dev.gaborbiro.dailymacros.features.common.views

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.util.randomBitmap
import java.io.ByteArrayInputStream
import java.io.InputStream


internal object PreviewImageStore : ImageStore {
    override suspend fun read(filename: String, thumbnail: Boolean): Bitmap? =
        randomBitmap(if (thumbnail) 256 else 1024, if (thumbnail) 256 else 1024)

    override suspend fun write(filename: String, bitmap: Bitmap) { /* no-op */
    }

    override suspend fun delete(filename: String) { /* no-op */
    }

    override suspend fun open(filename: String, thumbnail: Boolean): InputStream =
        ByteArrayInputStream(byteArrayOf()) // not used in previews
}

@Composable
fun PreviewImageStoreProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalImageStore provides PreviewImageStore) {
        content()
    }
}
