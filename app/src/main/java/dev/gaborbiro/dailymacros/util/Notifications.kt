package dev.gaborbiro.dailymacros.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import dev.gaborbiro.dailymacros.R

private const val CHANNEL_ID_DAILY_MACROS = "daily_macros"
private const val CHANNEL_ID_GENERAL = "general"
private const val NOTIFICATION_ID_DAILY_MACROS = 1001

fun Context.createNotificationChannels() {
    val macrosChannel = NotificationChannel(
        CHANNEL_ID_DAILY_MACROS,
        "Permanent notification for today's macro-nutrient intake",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        setSound(null, null)
        enableVibration(false)
        vibrationPattern = longArrayOf(0L)
        setShowBadge(false)
        lockscreenVisibility = Notification.VISIBILITY_SECRET
    }
    val generalChannel = NotificationChannel(
        CHANNEL_ID_GENERAL,
        "General notifications",
        NotificationManager.IMPORTANCE_DEFAULT
    )

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannels(
        listOf(
            macrosChannel, generalChannel
        )
    )
}


fun Context.setMacrosPermaNotification(message: String) {
    val builder = NotificationCompat.Builder(this, CHANNEL_ID_DAILY_MACROS)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Daily macros")
        .setContentText(message)
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(message)
        )
        .setOngoing(true)
        .setAutoCancel(false)
        .setVisibility(NotificationCompat.VISIBILITY_SECRET)
    getSystemService(NotificationManager::class.java).notify(
        NOTIFICATION_ID_DAILY_MACROS,
        builder.build()
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

//fun Context.hideActionNotification() {
//    getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID_ACTIONS)
//}
