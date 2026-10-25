package dev.gaborbiro.dailymacros.features.shared.mealreminders

import android.content.Context
import android.util.Log
import androidx.core.os.bundleOf
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.features.common.utils.logicalDiaryDate
import dev.gaborbiro.dailymacros.features.shared.mealreminders.MealReminderPlanner.windowEnd
import dev.gaborbiro.dailymacros.features.shared.mealreminders.MealReminderSchedule.isStale
import dev.gaborbiro.dailymacros.repositories.settings.domain.MealReminderRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One pass of the meal reminder loop, safe to run any time (app start, a diary change, the
 * scheduled wake-up): settle earlier reminders' outcomes, clear reminders whose meal got logged,
 * post the ones that are due, and schedule the next wake-up. Every pass re-plans from scratch,
 * so nothing goes stale when meals are logged, edited or deleted in between.
 *
 * A singleton so that every caller shares the one [mutex].
 */
@Singleton
class RunMealReminderPassUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val getMealReminderInputsUseCase: GetMealReminderInputsUseCase,
    private val mealReminderRepository: MealReminderRepository,
    private val notifier: MealReminderNotifier,
    private val analyticsLogger: AnalyticsLogger,
) {

    // The app-start pass, the diary observer and the alarm can overlap; only one may post.
    private val mutex = Mutex()

    /** [trigger] only feeds the Settings > Debug readout: what started this pass. */
    suspend fun execute(now: ZonedDateTime = ZonedDateTime.now(), trigger: String) = mutex.withLock {
        if (!mealReminderRepository.isEnabled()) {
            MealReminderAlarm.cancel(appContext)
            notifier.cancelAll()
            mealReminderRepository.setLastPass(now, "$trigger: reminders off", nextWakeUp = null)
            return@withLock
        }
        val (records, dayStart, loggedDayRule) = getMealReminderInputsUseCase.execute(now)

        val previous = mealReminderRepository.history()
        val history = MealReminderPlanner.resolveOutcomes(records, now, dayStart, previous).toMutableMap()
        history.forEach { (slot, resolved) ->
            if (previous[slot]?.awaitingOutcome == true && !resolved.awaitingOutcome) {
                analyticsLogger.logEvent(
                    "meal_reminder_outcome",
                    bundleOf("slot" to slot.name.lowercase(), "logged" to (resolved.consecutiveIgnored == 0)),
                )
            }
        }

        val plans = MealReminderPlanner.plan(records, now, dayStart, loggedDayRule, history)
        plans
            .filter { it.isAlreadyLogged() }
            .forEach { notifier.cancel(it.slot) }

        val due = MealReminderSchedule.due(plans, now)
        val canPost = notifier.canPost()
        val posted = if (canPost) due else emptyList()
        posted.forEach { (slot, remind) ->
            notifier.post(slot, remind.style, now, slot.windowEnd(now.logicalDiaryDate(dayStart), dayStart, now))
            history[slot] = history.getValue(slot).copy(lastRemindedAt = now, awaitingOutcome = true)
            analyticsLogger.logEvent(
                "meal_reminder_shown",
                bundleOf("slot" to slot.name.lowercase(), "style" to remind.style.name.lowercase()),
            )
        }
        mealReminderRepository.saveHistory(history)

        val next = MealReminderSchedule.nextCheck(plans, now, dayStart, excluding = due.map { it.first }.toSet())
        MealReminderAlarm.schedule(appContext, next)

        val dropped = plans.mapNotNull { plan -> plan.staleReminder(now)?.let { plan.slot to it } }
        val summary = buildList {
            if (posted.isNotEmpty()) add("posted ${posted.joinToString { it.first.label() }}")
            if (due.isNotEmpty() && !canPost) add("${due.joinToString { it.first.label() }} due but notifications blocked")
            dropped.forEach { (slot, remind) ->
                val lateBy = Duration.between(remind.at, now).toMinutes()
                add("dropped ${slot.label()} (${lateBy}m late)")
            }
        }.ifEmpty { listOf("nothing due") }.joinToString("; ")
        mealReminderRepository.setLastPass(now, "$trigger: $summary", nextWakeUp = next)
        Log.i(TAG, "$trigger: $summary, next check at ${next.format(DateTimeFormatter.ISO_LOCAL_TIME)}")
    }

    private fun MealSlotPlan.isAlreadyLogged() = when (val decision = decision) {
        is MealReminderDecision.Silent -> decision.reason == SilenceReason.ALREADY_LOGGED
        is MealReminderDecision.Remind -> false
    }

    /** The reminder that was due but is now too late to post, if any. */
    private fun MealSlotPlan.staleReminder(now: ZonedDateTime) = when (val decision = decision) {
        is MealReminderDecision.Remind -> decision.takeIf { it.isStale(now) }
        is MealReminderDecision.Silent -> null
    }

    private fun MealSlot.label() = name.lowercase()

    private companion object {
        const val TAG = "MealReminders"
    }
}
