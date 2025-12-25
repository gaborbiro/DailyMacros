package dev.gaborbiro.dailymacros.features.settings.export

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

class StreamWriter(
    private val context: Context,
) {
    suspend fun execute(
        uri: Uri,
        write: suspend (OutputStream) -> Unit,
    ) = withContext(Dispatchers.IO) {
        context.contentResolver
            .openOutputStream(uri, "w")
            ?.use { output ->
                write(output)
                output.flush()
            }
            ?: error("Unable to open output stream for uri=$uri")
    }
}
