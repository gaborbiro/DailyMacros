package dev.gaborbiro.dailymacros.features.common.views

import android.graphics.Bitmap
import android.graphics.Color
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import java.io.ByteArrayInputStream
import java.io.InputStream

object DummyImageStore : ImageStore {
    override suspend fun read(filename: String, thumbnail: Boolean): Bitmap? =
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }

    override suspend fun write(filename: String, bitmap: Bitmap) { /* no-op */ }

    override suspend fun delete(filename: String) { /* no-op */ }

    override suspend fun open(filename: String, thumbnail: Boolean): InputStream =
        ByteArrayInputStream(byteArrayOf())
}
