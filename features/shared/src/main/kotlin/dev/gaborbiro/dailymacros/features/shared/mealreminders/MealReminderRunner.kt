package dev.gaborbiro.dailymacros.features.shared.mealreminders

import android.content.Context
import android.util.Log
import androidx.core.os.bundleOf
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.features.common.utils.diaryDayStartTime
import dev.gaborbiro.dailymacros.features.common.utils.logicalDiaryDate
import dev.gaborbiro.dailymacros.features.shared.mealreminders.MealReminderPlanner.windowEnd
import dev.gaborbiro.dailymacros.features.shared.mealreminders.MealReminderSchedule.isStale
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.toKotlinInstant

/**
 * One pass of the meal reminder loop, safe to run any time (app start, a diary change, the
 * scheduled wake-up): settle earlier reminders' outcomes, clear reminders whose meal got logged,
 * post the ones that are due, and schedule the next wake-up. Every pass re-plans from scratch,
 * so nothing goes stale when meals are logged, edited or deleted in between.
 */
@Singleton
class MealReminderRunner @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val recordsRepository: RecordsRepository,
    private val settingsRepository: SettingsRepository,
    private val loggedDayRuleProvider: LoggedDayRuleProvider,
    private val prefs: MealReminderPrefs,
    private val notifier: MealReminderNotifier,
    private val analyticsLogger: AnalyticsLogger,
) {

    // The app-start pass, the diary observer and the alarm can overlap; only one may post.
    private val mutex = Mutex()

    data class Status(
        val enabled: Boolean,
        val notificationsAllowed: Boolean,
        val lastPass: MealReminderPrefs.LastPass?,
    )

    fun status() = Status(prefs.enabled, notifier.canPost(), prefs.lastPass)

    /** [trigger] only feeds the Settings > Debug readout: what started this pass. */
    suspend fun run(now: ZonedDateTime = ZonedDateTime.now(), trigger: String) = mutex.withLock {
        if (!prefs.enabled) {
            MealReminderAlarm.cancel(appContext)
            notifier.cancelAll()
            prefs.lastPass = MealReminderPrefs.LastPass(now, "$trigger: reminders off", nextWakeUp = null)
            return@withLock
        }
        val dayStart = diaryDayStartTime(settingsRepository.getDiaryDayStartHour())
        val records = recordsRepository.getRecords(
            since = now.minusDays(MealReminderPlanner.LOOKBACK_DAYS + 2).toInstant().toKotlinInstant(),
        )

        val previous = prefs.history()
        val history = MealReminderPlanner.resolveOutcomes(records, now, dayStart, previous).toMutableMap()
        history.forEach { (slot, resolved) ->
            if (previous[slot]?.awaitingOutcome == true && !resolved.awaitingOutcome) {
                analyticsLogger.logEvent(
                    "meal_reminder_outcome",
                    bundleOf("slot" to slot.name.lowercase(), "logged" to (resolved.consecutiveIgnored == 0)),
                )
            }
        }

        val plans = MealReminderPlanner.plan(records, now, dayStart, loggedDayRuleProvider.get(), history)
        plans
            .filter { (it.decision as? MealReminderDecision.Silent)?.reason == SilenceReason.ALREADY_LOGGED }
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
        prefs.saveHistory(history)

        val next = MealReminderSchedule.nextCheck(plans, now, dayStart, excluding = due.map { it.first }.toSet())
        MealReminderAlarm.schedule(appContext, next)

        val dropped = plans.filter { (it.decision as? MealReminderDecision.Remind)?.isStale(now) == true }
        val summary = buildList {
            if (posted.isNotEmpty()) add("posted ${posted.joinToString { it.first.label() }}")
            if (due.isNotEmpty() && !canPost) add("${due.joinToString { it.first.label() }} due but notifications blocked")
            dropped.forEach { plan ->
                val lateBy = Duration.between((plan.decision as MealReminderDecision.Remind).at, now).toMinutes()
                add("dropped ${plan.slot.label()} (${lateBy}m late)")
            }
        }.ifEmpty { listOf("nothing due") }.joinToString("; ")
        prefs.lastPass = MealReminderPrefs.LastPass(now, "$trigger: $summary", nextWakeUp = next)
        Log.i(TAG, "$trigger: $summary, next check at ${next.format(DateTimeFormatter.ISO_LOCAL_TIME)}")
    }

    private fun MealSlot.label() = name.lowercase()

    private companion object {
        const val TAG = "MealReminders"
    }
}
