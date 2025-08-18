package dev.gaborbiro.dailymacros.data.file

import android.content.Context
import java.io.File

internal class FileDestinationProviderPermanent(
    private val context: Context,
    folder: String,
) : FileDestinationProvider(context, folder) {

    /**
     * Provides the following File:
     * /data/data/dev.gaborbiro.dailymacros/files/[folder]/[filePath]
     *
     * Files in the /files folder stay around forever (or until the app is uninstalled).
     * Clear Cache won't clear them. This is good for storing data on the long term, but it is
     * advisable to implement a cleanup strategy. See [FileStore.delete].
     *
     * @param filePath using the same path again will override the file.
     *
     * @see android.content.Context.getFilesDir
     */
    override fun getBaseFile(filePath: String): File {
        return File(context.filesDir, filePath)
    }

    override fun newInstance(folder: String): FileDestinationProvider {
        return FileDestinationProviderPermanent(context, folder)
    }
}
