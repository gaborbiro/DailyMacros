package dev.gaborbiro.dailymacros.features.shared.mealreminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * The scheduled wake-up of [RunMealReminderPassUseCase]: an inexact alarm with a [WINDOW_MS] window, so
 * no exact-alarm permission is needed. WorkManager was tried first, but a job can be held back
 * for an hour or more (Doze, app standby), which a meal reminder can't absorb. A windowed alarm
 * fires on time while the phone is in use, and as soon as it leaves Doze otherwise.
 */
object MealReminderAlarm {

    /** Android 12+ won't go below 10 minutes anyway. */
    private const val WINDOW_MS = 10 * 60 * 1000L
    private const val REQUEST_CODE = 0x4D52 // "MR"
    const val ACTION_ALARM = "dev.gaborbiro.dailymacros.ACTION_MEAL_REMINDER_ALARM"

    /** Replaces the pending alarm, if any: each pass plans from scratch, so only the latest matters. */
    fun schedule(context: Context, at: ZonedDateTime) {
        context.getSystemService(AlarmManager::class.java)
            .setWindow(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), WINDOW_MS, pendingIntent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, MealReminderAlarmReceiver::class.java).setAction(ACTION_ALARM),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/**
 * Runs a reminder pass when [MealReminderAlarm] fires, and re-arms it after a reboot or app
 * update (alarms don't survive either).
 */
@AndroidEntryPoint
class MealReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var runMealReminderPassUseCase: RunMealReminderPassUseCase

    @Inject lateinit var analyticsLogger: AnalyticsLogger

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MealReminderAlarm.ACTION_ALARM,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
                -> {
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    try {
                        runMealReminderPassUseCase.execute(trigger = intent.action.orEmpty().substringAfterLast('.'))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e("MealReminders", "Meal reminder pass failed", e)
                        analyticsLogger.logError(e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
