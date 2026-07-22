package dev.gaborbiro.dailymacros.features.common.views

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.random.Random

object DummyImageStore : ImageStore {
    override suspend fun read(filename: String, thumbnail: Boolean): Bitmap =
        createBitmap(100, 100).apply {
            eraseColor(randomColor())
        }

    override suspend fun write(filename: String, bitmap: Bitmap) { /* no-op */
    }

    override suspend fun delete(filename: String) { /* no-op */
    }

    override suspend fun open(filename: String, thumbnail: Boolean): InputStream =
        ByteArrayInputStream(byteArrayOf())
}

fun randomColor(): Int = Color.argb(255, Random.nextInt(255), Random.nextInt(255), Random.nextInt(255))