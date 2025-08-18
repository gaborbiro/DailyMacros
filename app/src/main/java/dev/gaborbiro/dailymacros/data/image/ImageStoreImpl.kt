package dev.gaborbiro.dailymacros.data.image

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.util.LruCache
import androidx.core.graphics.scale
import dev.gaborbiro.dailymacros.DefaultFoodPicFormat
import dev.gaborbiro.dailymacros.DefaultFoodPicQuality
import dev.gaborbiro.dailymacros.data.file.FileStore
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTApiError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

internal class ImageStoreImpl(
    private val fileStore: FileStore,
    private val maxThumbnailSizePx: Int = 256, // bound for the longer edge
    private val foodPicFormat: Bitmap.CompressFormat = DefaultFoodPicFormat,
    private val foodPicQuality: Int = DefaultFoodPicQuality,

    ) : ImageStore {

    private val io = Dispatchers.IO
    private val cacheMutex = Mutex()

    private var fullCache: LruCache<String, Bitmap>
    private var thumbCache: LruCache<String, Bitmap>

    companion object {
        const val THUMBNAIL_SUFFIX = "-thumb"
    }

    init {
        val maxMemoryBytes = (Runtime.getRuntime().maxMemory()).toInt()
        val cacheSize = maxMemoryBytes / 6 // ~16% heap: adjust if needed
        fullCache = object : LruCache<String, Bitmap>(/* maxSize = */ cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap) = bitmap.byteCount
        }
        thumbCache = object : LruCache<String, Bitmap>(/* maxSize = */ cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap) = bitmap.byteCount
        }
    }

    // ---------- Public API (suspend, main-safe) ----------

    override suspend fun open(filename: String, thumbnail: Boolean): InputStream = withContext(io) {
        val finalFilename = if (thumbnail) thumbName(filename) else filename
        val path = fileStore.resolveFilePath(finalFilename)
        if (File(path).exists().not()) throw ChatGPTApiError.GenericApiError("File not found: $path")
        fileStore.read(finalFilename) // returns InputStream
    }

    override suspend fun read(filename: String, thumbnail: Boolean): Bitmap? = withContext(io) {
        val key = if (thumbnail) thumbName(filename) else filename

        // cache hit?
        cacheMutex.withLock {
            (if (thumbnail) thumbCache.get(key) else fullCache.get(key))?.let { return@withContext it }
        }

        // decode from disk (or generate thumb)
        val bmp: Bitmap? = if (thumbnail) {
            // Prefer existing thumb on disk
            decodeIfExists(thumbName(filename)) ?: decodeAndCreateThumb(filename)
        } else {
            decodeIfExists(filename)
        }

        // cache or purge on miss
        cacheMutex.withLock {
            if (bmp != null) {
                if (thumbnail) thumbCache.put(key, bmp) else fullCache.put(key, bmp)
            } else {
                if (thumbnail) thumbCache.remove(key) else fullCache.remove(key)
            }
        }
        bmp
    }

    override suspend fun write(filename: String, bitmap: Bitmap): Unit = withContext(io) {
        fileStore.write(filename) { out ->
            bitmap.compress(
                /* format = */ foodPicFormat,
                /* quality = */ foodPicQuality,
                /* stream = */ out
            )
        }

        // Eagerly create/update thumbnail for faster subsequent reads
        try {
            val thumb = createBoundedCopy(bitmap, maxThumbnailSizePx)
            fileStore.write(thumbName(filename)) { out ->
                thumb.compress(foodPicFormat, foodPicQuality, out)
            }
            cacheMutex.withLock {
                thumbCache.put(thumbName(filename), thumb)
                fullCache.put(filename, bitmap)
            }
        } catch (_: Throwable) {
            // ignore thumb generation errors; full image is already written
        }
    }

    override suspend fun delete(filename: String): Unit = withContext(io) {
        fileStore.delete(filename)
        fileStore.delete(thumbName(filename))
        cacheMutex.withLock {
            fullCache.remove(filename)
            thumbCache.remove(thumbName(filename))
        }
    }

    // ---------- Internals ----------

    private fun decodeIfExists(filename: String): Bitmap? {
        val path = fileStore.resolveFilePath(filename)
        val file = File(path)
        if (!file.exists()) return null
        val src = ImageDecoder.createSource(file)
        return ImageDecoder.decodeBitmap(src)
    }

    /** Decode full image and create/write a bounded thumbnail copy on disk. */
    private fun decodeAndCreateThumb(filename: String): Bitmap? {
        val path = fileStore.resolveFilePath(filename)
        val file = File(path)
        if (!file.exists()) return null

        // One pass decode + scale via ImageDecoder target size
        val src = ImageDecoder.createSource(file)
        val thumb = ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            val (w, h) = info.size.width to info.size.height
            val (tw, th) = boundSize(w, h, maxThumbnailSizePx)
            if (tw > 0 && th > 0 && (tw < w || th < h)) {
                decoder.setTargetSize(tw, th)
            }
        }

        // Persist thumb
        fileStore.write(thumbName(filename)) { out ->
            thumb.compress(foodPicFormat, foodPicQuality, out)
        }
        return thumb
    }

    /** Create a bounded copy from an existing Bitmap (used on write). */
    private fun createBoundedCopy(src: Bitmap, maxEdge: Int): Bitmap {
        val w = src.width
        val h = src.height
        val (tw, th) = boundSize(w, h, maxEdge)
        if (tw == w && th == h) return src
        return src.scale(tw, th)
    }

    /** Returns a size bounded to maxEdge while preserving aspect ratio. */
    private fun boundSize(w: Int, h: Int, maxEdge: Int): Pair<Int, Int> {
        if (w <= 0 || h <= 0) return 0 to 0
        if (w <= maxEdge && h <= maxEdge) return w to h
        return if (w >= h) {
            val tw = maxEdge
            val th = (h.toFloat() / w * tw).toInt().coerceAtLeast(1)
            tw to th
        } else {
            val th = maxEdge
            val tw = (w.toFloat() / h * th).toInt().coerceAtLeast(1)
            tw to th
        }
    }

    /**
     * Inserts [suffix] before the extension in [filename], or appends it if there's no extension.
     *
     * Rules:
     *  - "photo.png" + "-thumb" -> "photo-thumb.png"
     *  - "archive.tar.gz" + "-small" -> "archive.tar-small.gz"
     *  - "readme" + "-v2" -> "readme-v2"
     *  - ".gitignore" + "-old" -> ".gitignore-old"  (leading dot not treated as extension)
     *  - "2025-07-28T20:50:19.901788.png" + "-thumb" -> "2025-07-28T20:50:19.901788-thumb.png"
     */
    private fun thumbName(filename: String, suffix: String = THUMBNAIL_SUFFIX): String {
        if (filename.isEmpty()) return filename
        val lastDot = filename.lastIndexOf('.')
        return if (lastDot > 0) {
            val name = filename.substring(0, lastDot)
            val ext = filename.substring(lastDot)
            "$name$suffix$ext"
        } else {
            "$filename$suffix"
        }
    }
}
