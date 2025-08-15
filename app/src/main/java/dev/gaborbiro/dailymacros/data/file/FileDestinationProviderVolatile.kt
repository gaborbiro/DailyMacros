package dev.gaborbiro.dailymacros.data.file

import android.content.Context
import java.io.File

internal class FileDestinationProviderVolatile(
    private val context: Context,
    folder: String,
) : FileDestinationProvider(context, folder) {

    /**
     * Provides the following File:
     * /data/data/com.somnologymd.smdapp[.dev/.qa]?/cache/[folder]/[filePath]
     *
     * Everything in the cache folder is deleted when the app is uninstalled. There is also the
     * possibility of the files being deleted in a low-storage scenario or by Clear Cache, so don't
     * count on it being there forever. This is not a permanent file storage (use [FileDestinationProviderPermanent]
     * for that).
     *
     * @param filePath using the same path again will override the file.
     *
     * @see android.content.Context.getCacheDir
     */
    override fun getBaseFile(filePath: String): File {
        return File(context.cacheDir, filePath)
    }

    override fun newInstance(folder: String): FileDestinationProvider {
        return FileDestinationProviderVolatile(context, folder)
    }
}
