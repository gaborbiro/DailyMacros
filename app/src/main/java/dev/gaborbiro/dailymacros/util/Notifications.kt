package dev.gaborbiro.dailymacros.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import dev.gaborbiro.dailymacros.R

private const val CHANNEL_ID_GENERAL = "general"

fun Context.createNotificationChannels() {
    val generalChannel = NotificationChannel(
        CHANNEL_ID_GENERAL,
        "General notifications",
        NotificationManager.IMPORTANCE_DEFAULT
    )

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannels(
        listOf(
            generalChannel
        )
    )
}

fun Context.showSimpleNotification(id: Long, title: String, message: String) {
    val builder = NotificationCompat.Builder(this, CHANNEL_ID_GENERAL)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentText(message)
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(message)
                .setSummaryText(title)
        )
    getSystemService(NotificationManager::class.java).notify(
        id.toInt(),
        builder.build()
    )
}
