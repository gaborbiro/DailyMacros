package dev.gaborbiro.dailymacros.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import dev.gaborbiro.dailymacros.R

private const val CHANNEL_ID_GENERAL = "general"
const val CHANNEL_ID_FOREGROUND = "foreground"

fun Context.createNotificationChannels() {
    val generalChannel = NotificationChannel(
        CHANNEL_ID_GENERAL,
        "General notifications",
        NotificationManager.IMPORTANCE_DEFAULT
    )
    val foregroundChannel = NotificationChannel(
        CHANNEL_ID_FOREGROUND,
        "Background process",
        NotificationManager.IMPORTANCE_DEFAULT
    )
        .apply {
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

fun Context.showSimpleNotification(id: Long, title: String?, message: String?) {
    var builder = NotificationCompat.Builder(this, CHANNEL_ID_GENERAL)
        .setSmallIcon(R.drawable.ic_nutrition)
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
