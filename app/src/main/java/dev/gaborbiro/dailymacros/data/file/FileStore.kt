package dev.gaborbiro.dailymacros.data.file

import android.net.Uri
import java.io.InputStream
import java.io.OutputStream

interface FileStore {

    /**
     * Do not call this on the main thread.
     *
     * @param outputFilename re-using the same name will override the file (unless [append] is set)
     *
     * @return android.net.Uri but in a String format. Use android.net.Uri.parse(this) or androidx.core.net.toUri() to convert it to Uri.
     */
    fun writeBlocking(
        source: InputStream,
        outputFilename: String,
        append: Boolean = false,
    ): String

    /**
     * @return android.net.Uri but in a String format. Use android.net.Uri.parse(this) or androidx.core.net.toUri() to convert it to Uri.
     */
    fun getOrCreateFile(filename: String): String

    /**
     * @return local file path on the device
     */
    fun resolveFilePath(filename: String): String

    /**
     * @return android.net.Uri but in a String format. Use android.net.Uri.parse(this) or androidx.core.net.toUri() to convert it to Uri.
     */
    fun write(filename: String, onStream: (OutputStream) -> Unit): String

    fun read(filename: String): InputStream

    fun delete(filename: String)

    fun delete(uri: Uri)
}
