package dev.gaborbiro.dailymacros.features.settings.export

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamWriter @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    suspend fun execute(
        uri: Uri,
        write: suspend (OutputStream) -> Unit,
    ) = withContext(Dispatchers.IO) {
        appContext.contentResolver
            .openOutputStream(uri, "w")
            ?.use { output ->
                write(output)
                output.flush()
            }
            ?: error("Unable to open output stream for uri=$uri")
    }
}
