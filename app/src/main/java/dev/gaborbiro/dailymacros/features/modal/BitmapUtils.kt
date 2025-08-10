package dev.gaborbiro.dailymacros.features.modal

import android.graphics.Bitmap
import android.util.Base64
import android.util.Base64OutputStream
import dev.gaborbiro.dailymacros.ImageFileFormat
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Encodes the input stream to a Base64 string. Does not decode image, just streams raw bytes.
 */
internal fun inputStreamToBase64(
    input: InputStream,
    format: Bitmap.CompressFormat = ImageFileFormat,
): String {
    val mimeType = when (format) {
        Bitmap.CompressFormat.PNG -> "image/png"
        Bitmap.CompressFormat.JPEG -> "image/jpeg"
        Bitmap.CompressFormat.WEBP -> "image/webp"
        else -> "application/octet-stream"
    }
    val baos = ByteArrayOutputStream()
    Base64OutputStream(baos, Base64.NO_WRAP).use { b64Out ->
        input.use { inp ->
            val buffer = ByteArray(8 * 1024)
            var read: Int
            while (inp.read(buffer).also { read = it } != -1) {
                b64Out.write(buffer, 0, read)
            }
            b64Out.flush()
        }
    }
    val base64 = baos.toString("UTF-8")
    return "data:$mimeType;base64,$base64"
}
