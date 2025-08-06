package dev.gaborbiro.nutri.store.file

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

internal abstract class FileDestinationProvider(
    private val context: Context,
    private val folder: String,
) {

    /**
     * Provides a content:// uri to a file. Ensures the parent folders exist.
     *
     * Files provided like this are not accessible from the outside by default, but can be made
     * temporarily readable/writable by other apps by adding the
     * FLAG_GRANT_READ_URI_PERMISSION/FLAG_GRANT_WRITE_URI_PERMISSION to your app-chooser Intent
     * (to open a PDF viewer for example). Check out the FileProvider in AndroidManifest.xml and
     * res/xml/file_paths.xml.
     *
     * The granted permissions are available to the client app for as long as the stack for a
     * receiving Activity is active. For an Intent going to a Service, the permissions are available
     * as long as the Service is running.
     *
     * @param filename using the same name again will override the file
     *
     * @throws IllegalArgumentException if the specified [filename] is a folder
     *
     * @see androidx.core.content.FileProvider
     */
    fun getContentUri(filename: String): Uri {
        return FileProvider.getUriForFile(
            /* context = */ context,
            /* authority = */ "${context.packageName}.provider",
            /* file = */ getFile(filename).also {
                // let's create any missing folders
                it.parentFile?.mkdirs()
            })
    }

    fun delete(filename: String) {
        getFile(filename).delete()
    }

    fun getFile(filename: String): File {
        return getBaseFile(addPaths(folder, filename))
    }

    /**
     * Provides a file for reading/writing.
     *
     * This file is not accessible by default by other apps, but can be made temporarily
     * readable/writable by adding the FLAG_GRANT_READ_URI_PERMISSION/FLAG_GRANT_WRITE_URI_PERMISSION
     * to your app-chooser Intent (to open a PDF viewer for example). Check out the FileProvider in
     * AndroidManifest.xml and res/xml/file_paths.xml.
     *
     * The granted permissions are available to the client app for as long as the stack for a
     * receiving Activity is active. For an Intent going to a Service, the permissions are available
     * as long as the Service is running.
     *
     * The exact nature of the file's location depends on the implementation.
     *
     * @param filePath using the same path again will override the file
     */
    internal abstract fun getBaseFile(filePath: String): File

    private fun addPaths(path1: String, path2: String): String {
        val sep = File.separator
        return (path1.trim(sep[0]) + sep + path2.trim(sep[0])).trim(sep[0])
    }

    internal abstract fun newInstance(folder: String): FileDestinationProvider
}
