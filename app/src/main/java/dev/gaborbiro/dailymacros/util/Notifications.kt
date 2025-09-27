package dev.gaborbiro.dailymacros.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.features.main.MainActivity
import dev.gaborbiro.dailymacros.features.modal.getViewRecordDetailsIntent

private const val CHANNEL_ID_GENERAL = "general"
const val CHANNEL_ID_FOREGROUND = "foreground"

fun Context.createNotificationChannels() {
    val generalChannel = NotificationChannel(
        CHANNEL_ID_GENERAL,
        "General notifications",
        NotificationManager.IMPORTANCE_DEFAULT
    ).also {
        it.setShowBadge(false)
    }
    val foregroundChannel = NotificationChannel(
        CHANNEL_ID_FOREGROUND,
        "Background process",
        NotificationManager.IMPORTANCE_DEFAULT
    )
        .apply {
            setShowBadge(false)
            description =
                "Notifications required for the app to be able to work when not visible (for example fetching macro-nutrient information after adding a meal via the widget)"
        }

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannels(
        listOf(
            generalChannel,
            foregroundChannel
        )
    )
}

fun Context.showMacroResultsNotification(
    id: Long,
    recordId: Long,
    title: String?,
    message: String?,
) {
    var builder = NotificationCompat.Builder(this, CHANNEL_ID_GENERAL)
        .setSmallIcon(R.drawable.ic_nutrition)
        .setContentIntent(openRecordDetailsIntent(recordId))
//        .setAutoCancel(true)
    message?.let {
        builder = builder.setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setSummaryText(title ?: "")
            )
    }
    getSystemService(NotificationManager::class.java).notify(
        id.toInt(),
        builder.build()
    )
}

private fun Context.openRecordDetailsIntent(recordId: Long): PendingIntent? {
    val modalIntent = getViewRecordDetailsIntent(recordId)
    modalIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

    return PendingIntent.getActivities(
        this,
        recordId.toInt(),
        arrayOf(modalIntent),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
