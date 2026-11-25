package dev.gaborbiro.dailymacros.data.image.domain

import android.graphics.Bitmap
import java.io.InputStream

interface ImageStore {

    suspend fun open(filename: String, thumbnail: Boolean): InputStream

    suspend fun read(filename: String, thumbnail: Boolean): Bitmap?

    /**
     * The photo decoded to at most [maxEdgePx] on its longer edge: sharper than the thumbnail for
     * tiles bigger than it, lighter than the full photo. Falls back to the thumbnail when the full
     * photo is gone.
     */
    suspend fun readScaled(filename: String, maxEdgePx: Int): Bitmap?

    suspend fun write(filename: String, bitmap: Bitmap)

    suspend fun delete(filename: String)
}
