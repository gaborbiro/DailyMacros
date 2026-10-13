package dev.gaborbiro.dailymacros.features.shared.mealreminders

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration

/**
 * The scheduled wake-up of [MealReminderRunner]. WorkManager rather than AlarmManager: it survives
 * reboots and app updates without a boot receiver, and needs no exact-alarm permission. The cost
 * is precision - a delayed run can land a few minutes late, which a meal reminder can live with.
 */
@HiltWorker
class MealReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val runner: MealReminderRunner,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        runner.run()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "meal_reminders"

        /**
         * Replaces whatever wake-up was pending: each run plans from scratch, so only the latest
         * schedule matters. When the worker reschedules itself this also cancels the finishing
         * run, which is harmless as the reschedule is its last step.
         */
        fun schedule(context: Context, delay: Duration) {
            val request = OneTimeWorkRequestBuilder<MealReminderWorker>()
                .setInitialDelay(delay.coerceAtLeast(Duration.ZERO))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
