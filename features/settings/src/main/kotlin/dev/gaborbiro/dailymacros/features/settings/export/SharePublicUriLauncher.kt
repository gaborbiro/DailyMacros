package dev.gaborbiro.dailymacros.features.settings.export

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Launches a share intent for a user-exported document.
 *
 * Guarantees:
 * - Adds FLAG_GRANT_READ_URI_PERMISSION to the share intent
 * - Adds FLAG_ACTIVITY_NEW_TASK to the chooser (required when starting an
 *   activity from a non-Activity context)
 *
 * Preconditions:
 * - Uri must be a content:// Uri
 * - Uri must refer to a document intended for external sharing
 *   (e.g. obtained via SAF CreateDocument)
 *
 * Do NOT use with:
 * - file:// Uris
 * - app-private files
 * - temporary cache Uris
 */
@Singleton
class SharePublicUriLauncher @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    fun execute(
        uri: Uri,
        mimeType: String = "application/json",
        chooserTitle: String = "Share file",
    ) {
        check(uri.scheme == ContentResolver.SCHEME_CONTENT) {
            "Only content:// Uris can be shared safely. Got: $uri"
        }

        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(send, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        appContext.startActivity(chooser)
    }
}
