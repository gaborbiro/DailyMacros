package dev.gaborbiro.dailymacros.features.settings.export

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import dev.gaborbiro.dailymacros.features.settings.R
import dev.gaborbiro.dailymacros.features.shared.notifications.CHANNEL_ID_FOREGROUND
import dev.gaborbiro.dailymacros.features.shared.notifications.CHANNEL_ID_GENERAL
import dev.gaborbiro.dailymacros.features.shared.R as sharedR

/**
 * Notifications for the background PDF export. The progress notification is replaced in place by the
 * completion (Open / Share) or error notification, keyed by [notificationId].
 */
object ExportNotifications {

    private const val MIME_PDF = "application/pdf"

    fun showProgress(context: Context, notificationId: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FOREGROUND)
            .setSmallIcon(sharedR.drawable.ic_nutrition)
            .setContentTitle(context.getString(R.string.pdf_export_notification_progress_title))
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setSilent(true)
            .build()
        manager(context).notify(notificationId, notification)
    }

    fun showComplete(context: Context, notificationId: Int, uri: Uri) {
        val openIntent = pendingActivity(context, notificationId, viewIntent(uri))
        val shareIntent = pendingActivity(context, notificationId + 1, shareIntent(uri))

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setSmallIcon(sharedR.drawable.ic_nutrition)
            .setContentTitle(context.getString(R.string.pdf_export_notification_complete_title))
            .setContentText(context.getString(R.string.pdf_export_notification_complete_text))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.pdf_export_open), openIntent)
            .addAction(0, context.getString(R.string.pdf_export_share), shareIntent)
            .build()
        manager(context).notify(notificationId, notification)
    }

    fun showError(context: Context, notificationId: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setSmallIcon(sharedR.drawable.ic_nutrition)
            .setContentTitle(context.getString(R.string.pdf_export_notification_error_title))
            .setContentText(context.getString(R.string.pdf_export_notification_error_text))
            .setAutoCancel(true)
            .build()
        manager(context).notify(notificationId, notification)
    }

    private fun viewIntent(uri: Uri) = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, MIME_PDF)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun shareIntent(uri: Uri): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = MIME_PDF
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, null).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }

    private fun pendingActivity(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun manager(context: Context) =
        context.getSystemService(NotificationManager::class.java)
}
