package dev.gaborbiro.dailymacros.features.main

import android.app.Activity
import android.content.Intent
import android.net.Uri

class ShareIntentLauncher(
    private val activity: Activity,
) {

    fun execute(
        uri: Uri,
        mimeType: String = "application/json",
        chooserTitle: String = "Share file",
    ) {
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
