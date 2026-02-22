package dev.gaborbiro.dailymacros.features.modal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Encodes the input stream to a Base64 string. Does not decode image, just streams raw bytes.
 */
internal fun inputStreamToBase64(
    input: InputStream,
    maxSizePx: Int = 1024,               // max dimension for LLM upload
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    quality: Int = 80,                   // 75–85 recommended
): String {

    // 1️⃣ Decode bitmap
    val original = BitmapFactory.decodeStream(input)
        ?: error("Failed to decode image stream")

    // 2️⃣ Compute resize ratio (do not upscale)
    val ratio = minOf(
        maxSizePx.toFloat() / original.width,
        maxSizePx.toFloat() / original.height,
        1f
    )

    val targetWidth = (original.width * ratio).toInt()
    val targetHeight = (original.height * ratio).toInt()

    val resized =
        if (ratio < 1f) {
            original.scale(targetWidth, targetHeight)
        } else {
            original
        }

    // 3️⃣ Recompress deterministically
    val baos = ByteArrayOutputStream()
    resized.compress(format, quality, baos)

    val imageBytes = baos.toByteArray()

    // 4️⃣ Base64 encode
    val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

    val mimeType = when (format) {
        Bitmap.CompressFormat.PNG -> "image/png"
        Bitmap.CompressFormat.JPEG -> "image/jpeg"
        Bitmap.CompressFormat.WEBP -> "image/webp"
        else -> "application/octet-stream"
    }

    val result = "data:$mimeType;base64,$base64"
    Log.i("Image SHA-256", result.sha256())
    return result
}

internal fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(this.toByteArray(Charsets.UTF_8))

    return digest.joinToString("") { "%02x".format(it) }
}