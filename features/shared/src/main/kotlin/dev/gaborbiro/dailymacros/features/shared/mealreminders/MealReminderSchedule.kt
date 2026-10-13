package dev.gaborbiro.dailymacros.features.shared.mealreminders

import dev.gaborbiro.dailymacros.features.common.utils.nextDiaryDayBoundary
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Turns today's [MealSlotPlan]s into "post these now" and "wake up again at". Pure, like the
 * planner, so the timing edge cases are unit tested rather than discovered on a phone.
 */
object MealReminderSchedule {

    /** Background work can run a little early; a reminder this close counts as due. */
    val EARLY_TOLERANCE: Duration = Duration.ofMinutes(1)

    /**
     * A reminder whose time passed longer ago than this is dropped instead of posted, so turning
     * the feature on at 15:00 (or a phone waking from deep sleep) doesn't fire "lunch time?" late.
     */
    val MAX_LATENESS: Duration = Duration.ofMinutes(60)

    fun due(plans: List<MealSlotPlan>, now: ZonedDateTime): List<Pair<MealSlot, MealReminderDecision.Remind>> =
        plans.mapNotNull { plan ->
            (plan.decision as? MealReminderDecision.Remind)
                ?.takeIf { !it.at.isAfter(now.plus(EARLY_TOLERANCE)) && !it.at.isBefore(now.minus(MAX_LATENESS)) }
                ?.let { plan.slot to it }
        }

    /**
     * The next upcoming reminder, or the start of the next diary day (when tomorrow's plan
     * replaces today's), whichever comes first. Reminders in [excluding] (just posted) and ones
     * already due or stale are skipped, so a run never schedules itself again straight away.
     */
    fun nextCheck(
        plans: List<MealSlotPlan>,
        now: ZonedDateTime,
        dayStart: LocalTime,
        excluding: Set<MealSlot> = emptySet(),
    ): ZonedDateTime {
        val nextReminder = plans
            .filter { it.slot !in excluding }
            .mapNotNull { (it.decision as? MealReminderDecision.Remind)?.at }
            .filter { it.isAfter(now.plus(EARLY_TOLERANCE)) }
            .minByOrNull { it.toInstant() }
        val dayBoundary = nextDiaryDayBoundary(now, dayStart)
        return listOfNotNull(nextReminder, dayBoundary).minBy { it.toInstant() }
    }
}
