package dev.gaborbiro.dailymacros.features.shared.mealreminders

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.features.shared.ModalNavigator
import dev.gaborbiro.dailymacros.features.shared.ModalNavigator.AddRecordMode
import dev.gaborbiro.dailymacros.features.shared.R
import dev.gaborbiro.dailymacros.features.shared.notifications.CHANNEL_ID_MEAL_REMINDERS
import java.time.Duration
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts and clears the one-per-slot meal reminder notification. A photo prompt opens the camera
 * (the photo is the point); a catch-up opens the text entry, since by then the plate is usually
 * empty.
 */
@Singleton
class MealReminderNotifier @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val modalNavigator: ModalNavigator,
) {

    private val notificationManager get() = appContext.getSystemService(NotificationManager::class.java)

    /** False when the user blocked notifications (or never granted the Android 13+ permission). */
    fun canPost(): Boolean = NotificationManagerCompat.from(appContext).areNotificationsEnabled()

    fun post(slot: MealSlot, style: ReminderStyle, now: ZonedDateTime, slotEnd: ZonedDateTime) {
        val notificationId = slot.notificationId()
        val (title, text, tapMode, actionMode, actionLabel) = when (style) {
            ReminderStyle.PHOTO_PROMPT -> Content(
                title = slot.photoTitle(),
                text = R.string.meal_reminder_photo_text,
                tapMode = AddRecordMode.CAMERA,
                actionMode = AddRecordMode.TEXT_ONLY,
                actionLabel = R.string.meal_reminder_photo_action_describe,
            )

            ReminderStyle.CATCH_UP -> Content(
                title = slot.catchUpTitle(),
                text = R.string.meal_reminder_catch_up_text,
                tapMode = AddRecordMode.TEXT_ONLY,
                actionMode = AddRecordMode.BROWSE_IMAGES,
                actionLabel = R.string.meal_reminder_catch_up_action_photo,
            )
        }
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID_MEAL_REMINDERS)
            .setSmallIcon(R.drawable.ic_nutrition)
            .setContentTitle(appContext.getString(title))
            .setContentText(appContext.getString(text))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            // A breakfast nudge still hanging around at dinner is just clutter.
            .setTimeoutAfter(Duration.between(now, slotEnd).toMillis().coerceAtLeast(1L))
            .setContentIntent(pendingIntent(slot, tapMode, requestCodeOffset = 0))
            .addAction(0, appContext.getString(actionLabel), pendingIntent(slot, actionMode, requestCodeOffset = 1))
            .build()
        notificationManager.notify(notificationId, notification)
    }

    fun cancel(slot: MealSlot) = notificationManager.cancel(slot.notificationId())

    fun cancelAll() = MealSlot.entries.forEach(::cancel)

    private fun pendingIntent(slot: MealSlot, mode: AddRecordMode, requestCodeOffset: Int) =
        modalNavigator.addRecordPendingIntent(
            context = appContext,
            requestCode = slot.notificationId() * 10 + requestCodeOffset,
            mode = mode,
            notificationId = slot.notificationId(),
        )

    private data class Content(
        val title: Int,
        val text: Int,
        val tapMode: AddRecordMode,
        val actionMode: AddRecordMode,
        val actionLabel: Int,
    )

    private fun MealSlot.notificationId() = NOTIFICATION_ID_BASE + ordinal

    private fun MealSlot.photoTitle() = when (this) {
        MealSlot.BREAKFAST -> R.string.meal_reminder_photo_title_breakfast
        MealSlot.LUNCH -> R.string.meal_reminder_photo_title_lunch
        MealSlot.DINNER -> R.string.meal_reminder_photo_title_dinner
    }

    private fun MealSlot.catchUpTitle() = when (this) {
        MealSlot.BREAKFAST -> R.string.meal_reminder_catch_up_title_breakfast
        MealSlot.LUNCH -> R.string.meal_reminder_catch_up_title_lunch
        MealSlot.DINNER -> R.string.meal_reminder_catch_up_title_dinner
    }

    private companion object {
        const val NOTIFICATION_ID_BASE = 0x4D52 // "MR"
    }
}
