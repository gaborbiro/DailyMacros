package dev.gaborbiro.dailymacros.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.features.main.MainActivity
import dev.gaborbiro.dailymacros.features.shared.notifications.CHANNEL_ID_FOREGROUND

private const val CHANNEL_ID_GENERAL = "general"
const val CHANNEL_ID_ERROR = "error"

fun Context.createNotificationChannels() {
    val generalChannel = NotificationChannel(
        CHANNEL_ID_GENERAL,
        "General notifications",
        NotificationManager.IMPORTANCE_DEFAULT
    ).also {
        it.setShowBadge(false)
        it.setSound(null, null)
    }
    val errorChannel = NotificationChannel(
        CHANNEL_ID_ERROR,
        "Error notifications",
        NotificationManager.IMPORTANCE_HIGH
    ).also {
        it.setShowBadge(false)
    }
    val foregroundChannel = NotificationChannel(
        CHANNEL_ID_FOREGROUND,
        "Background process",
        NotificationManager.IMPORTANCE_LOW
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
            errorChannel,
            foregroundChannel,
        )
    )
}

fun Context.showMacroResultsNotification(
    id: Long,
    recordId: Long,
    title: String?,
    message: String,
    isError: Boolean,
) {
    val channelId = if (isError) CHANNEL_ID_ERROR else CHANNEL_ID_GENERAL
    var builder = NotificationCompat.Builder(this, channelId)
        .setSmallIcon(R.drawable.ic_nutrition)
        .setContentIntent(openOverviewIntent())
    title?.takeIf { it.isNotBlank() }?.let {
        builder = builder.setContentTitle(it)
    }
    builder = builder.setContentText(message)
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(message)
                .setSummaryText(title ?: "")
        )
    getSystemService(NotificationManager::class.java).notify(
        id.toInt(),
        builder.build()
    )
}

fun Context.showTitleTextNotification(
    id: Int,
    title: String,
    text: String,
    isError: Boolean,
) {
    val channelId = if (isError) CHANNEL_ID_ERROR else CHANNEL_ID_GENERAL
    val builder = NotificationCompat.Builder(this, channelId)
        .setSmallIcon(R.drawable.ic_nutrition)
        .setContentTitle(title)
        .setContentText(text)
        .setAutoCancel(true)
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(text),
        )
    getSystemService(NotificationManager::class.java).notify(id, builder.build())
}

fun Context.showTextNotification(
    id: Long,
    message: String?,
) {
    var builder = NotificationCompat.Builder(this, CHANNEL_ID_GENERAL)
        .setSmallIcon(R.drawable.ic_nutrition)
        .setAutoCancel(true)
    message?.let {
        builder = builder.setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
    }
    getSystemService(NotificationManager::class.java).notify(
        id.toInt(),
        builder.build()
    )
}

private const val NOTIFICATION_ID_AUTO_SYNC_FAILURE = 2001
private const val NOTIFICATION_ID_AUTO_SYNC_CONFLICT = 2002

fun Context.showAutoSyncConflictNotification() {
    val builder = NotificationCompat.Builder(this, CHANNEL_ID_ERROR)
        .setSmallIcon(R.drawable.ic_nutrition)
        .setContentTitle("Backup paused")
        .setContentText("Another device has newer backup data. Open Settings to decide what to do.")
        .setAutoCancel(true)
        .setContentIntent(openOverviewIntent())
    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID_AUTO_SYNC_CONFLICT, builder.build())
}

fun Context.showAutoSyncFailureNotification() {
    val builder = NotificationCompat.Builder(this, CHANNEL_ID_ERROR)
        .setSmallIcon(R.drawable.ic_nutrition)
        .setContentTitle("Backup failed")
        .setContentText("Cloud backup failed. Open Settings to fix.")
        .setAutoCancel(true)
        .setContentIntent(openOverviewIntent())
    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID_AUTO_SYNC_FAILURE, builder.build())
}

private fun Context.openOverviewIntent(): PendingIntent? {
    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
        this,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
