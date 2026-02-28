package dev.gaborbiro.dailymacros.features.settings.export

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

/**
 * Launches a share intent for a user-exported document.
 *
 * Guarantees:
 * - Adds FLAG_GRANT_READ_URI_PERMISSION to the share intent
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
class SharePublicUriLauncher(
    private val activity: Activity,
) {

    fun execute(
        uri: Uri,
        mimeType: String = "application/json",
        chooserTitle: String = "Share file",
    ) {
        check(uri.scheme == ContentResolver.SCHEME_CONTENT) {
            "Only content:// Uris can be shared safely. Got: $uri"
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        activity.startActivity(
            Intent.createChooser(intent, chooserTitle)
        )
    }
}
