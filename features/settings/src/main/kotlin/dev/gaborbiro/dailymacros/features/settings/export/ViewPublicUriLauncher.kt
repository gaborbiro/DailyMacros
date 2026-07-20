package dev.gaborbiro.dailymacros.features.settings.export

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens a user-exported document in the user's default viewer via [Intent.ACTION_VIEW].
 *
 * Used for the post-export "Open" action so users don't have to hunt for the saved file.
 * Same preconditions as [SharePublicUriLauncher]: the Uri must be a content:// Uri obtained via
 * SAF CreateDocument.
 */
@Singleton
class ViewPublicUriLauncher @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    fun execute(
        uri: Uri,
        mimeType: String = "application/pdf",
    ) {
        check(uri.scheme == ContentResolver.SCHEME_CONTENT) {
            "Only content:// Uris can be opened safely. Got: $uri"
        }

        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        appContext.startActivity(view)
    }
}
