package dev.gaborbiro.nutri.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import dev.gaborbiro.nutri.R

private const val CHANNEL_ID_ACTIONS = "actions"
private const val CHANNEL_ID_GENERAL = "general"
private const val NOTIFICATION_ID_ACTIONS = 1001
private const val NOTIFICATION_ID_GENERAL = 1002

fun Context.createNotificationChannels() {
    val undoChannel = NotificationChannel(
        CHANNEL_ID_ACTIONS,
        "Actions",
        NotificationManager.IMPORTANCE_DEFAULT
    )
    val generalChannel = NotificationChannel(
        CHANNEL_ID_GENERAL,
        "General",
        NotificationManager.IMPORTANCE_LOW
    )

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannels(
        listOf(
            undoChannel, generalChannel
        )
    )
}

fun Context.showActionNotification(
    title: String,
    action: String,
    @DrawableRes actionIcon: Int,
    actionIntent: Intent
) {
    val pendingIntent = PendingIntent.getActivity(
        applicationContext,
        1,
        actionIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val builder = NotificationCompat.Builder(this, CHANNEL_ID_ACTIONS)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .addAction(actionIcon, action, pendingIntent)
    getSystemService(NotificationManager::class.java).notify(
        NOTIFICATION_ID_ACTIONS,
        builder.build()
    )
}

fun Context.showSimpleNotification(id: Long, title: String) {
    val builder = NotificationCompat.Builder(this, CHANNEL_ID_GENERAL)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
    getSystemService(NotificationManager::class.java).notify(
        id.toInt(),
        builder.build()
    )

}

//fun Context.hideActionNotification() {
//    getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID_ACTIONS)
//}
