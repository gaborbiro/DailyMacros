package dev.gaborbiro.dailymacros.data.image.domain

import android.graphics.Bitmap
import java.io.InputStream

interface ImageStore {

    suspend fun open(filename: String, thumbnail: Boolean): InputStream

    suspend fun read(filename: String, thumbnail: Boolean): Bitmap?

    suspend fun write(filename: String, bitmap: Bitmap)

    suspend fun delete(filename: String)
}
