package dev.gaborbiro.dailymacros.data.file

import android.content.Context
import android.net.Uri
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


/**
 * Utilities for writing to a specified output destination. The exact location and
 * nature of the destination depends on the [destinationProvider] implementation.
 */
internal class FileStoreImpl(
    private val context: Context,
    private val destinationProvider: FileDestinationProvider,
) : FileStore {

    /**
     * Do not call this on the main thread.
     *
     * @param outputFilename re-using the same name will override the file (unless [append] is set)
     */
    override fun writeBlocking(
        source: InputStream,
        outputFilename: String,
        append: Boolean,
    ): String {
        val (outputStream, contentUri) = getOutputStream(outputFilename, append)
        doWrite(outputStream, source)
        return contentUri.toString()
    }

    private fun doWrite(
        outputStream: OutputStream,
        source: InputStream,
    ) {
        outputStream.use { output ->
            source.copyTo(output)
        }
    }

    override fun getOrCreateFile(filename: String): String {
        return destinationProvider.getContentUri(filename).toString()
    }

    override fun resolveFilePath(filename: String): String {
        return destinationProvider.getFile(filename).path
    }

    override fun write(filename: String, onStream: OutputStream.() -> Unit): String {
        val uri = destinationProvider.getContentUri(filename)
        context.contentResolver.openOutputStream(uri)
            ?.use(onStream)
            ?: throw IOException("ContentProvider recently crashed. Try again later.")
        return uri.toString()
    }

    override fun read(filename: String): InputStream {
        val uri = destinationProvider.getContentUri(filename)
        return context.contentResolver.openInputStream(uri)
            ?: throw IOException("ContentProvider recently crashed. Try again later.")
    }

    override fun delete(filename: String) {
        destinationProvider.delete(filename)
    }

    override fun delete(uri: Uri) {
        context.contentResolver.delete(uri, null, null)
    }

    /**
     * Important!! Free up the OutputStream after use by calling .close()
     */
    private fun getOutputStream(
        outputFilename: String,
        append: Boolean = false,
    ): Pair<OutputStream, Uri> {
        val uri = destinationProvider.getContentUri(outputFilename)
        val outputStream = context.contentResolver.openOutputStream(uri, if (append) "wa" else "w")
            ?: throw IOException("ContentProvider recently crashed. Try again later.")
        return outputStream to uri
    }
}
