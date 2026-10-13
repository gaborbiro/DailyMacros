package dev.gaborbiro.dailymacros.features.shared.mealreminders

import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Records carry no meal type, so a meal's slot comes from its wall-clock time. The windows are
 * deliberately wide and fixed: inferring slots from meal order ("2nd meal = lunch") falls apart
 * as soon as one meal goes unlogged, which is exactly the situation reminders exist for.
 *
 * Dinner runs until the diary day ends, so with a 2am day start a 00:30 meal is still dinner.
 */
enum class MealSlot(
    /** Minutes after the diary date's local midnight. */
    val windowStartMinute: Int,
    /** Used until enough history has been logged to learn the user's own time. */
    val defaultTime: LocalTime,
) {
    BREAKFAST(windowStartMinute = 5 * 60, defaultTime = LocalTime.of(8, 0)),
    LUNCH(windowStartMinute = 11 * 60, defaultTime = LocalTime.of(12, 30)),
    DINNER(windowStartMinute = 16 * 60, defaultTime = LocalTime.of(19, 0)),
}

/** What the reminder for one slot has done recently. Persisted by whoever posts the notifications. */
data class SlotReminderHistory(
    val lastRemindedAt: ZonedDateTime? = null,
    /** Reminders in a row that ended without the slot getting logged. Reset when one is acted on. */
    val consecutiveIgnored: Int = 0,
    /** The last reminder went out and its slot is neither logged nor over yet (see [MealReminderPlanner.resolveOutcomes]). */
    val awaitingOutcome: Boolean = false,
)

data class LearnedMealTime(
    /** Median start time of the meal, in minutes after the diary date's local midnight (may exceed 24h). */
    val typicalMinute: Int,
    /** 25th percentile - null when [basis] is [Basis.DEFAULT]. */
    val earlyMinute: Int?,
    /** 75th percentile - null when [basis] is [Basis.DEFAULT]. */
    val lateMinute: Int?,
    /** Meals of this slot found in the history (all days, regardless of the weekday/weekend split). */
    val sampleCount: Int,
    val basis: Basis,
    val consistency: Consistency,
) {
    enum class Basis { DEFAULT, ALL_DAYS, WEEKDAYS, WEEKENDS }

    enum class Consistency {
        /** Eats this meal at roughly the same time - remind at that time and ask for a photo. */
        TIGHT,

        /** All over the place - a "did you eat yet?" catch-up after the usual latest time works better. */
        LOOSE,

        /** Not enough history to tell. */
        UNKNOWN,
    }
}

enum class ReminderStyle {
    /** Lands when the meal usually starts: "snap it before you dig in". */
    PHOTO_PROMPT,

    /** Lands after the meal would normally be over: "had lunch yet? Log it, even without a photo". */
    CATCH_UP,
}

sealed interface MealReminderDecision {

    /** [at] may already be in the past (e.g. after a reboot); a scheduler should then post right away. */
    data class Remind(val at: ZonedDateTime, val style: ReminderStyle) : MealReminderDecision

    data class Silent(val reason: SilenceReason) : MealReminderDecision
}

enum class SilenceReason {
    /** A meal-sized (or still analysing) record is already in this slot today. */
    ALREADY_LOGGED,

    /** This slot's reminder already went out today. */
    ALREADY_REMINDED,

    /** Ignored too many times in a row; paused for a while so it doesn't turn into noise. */
    BACKED_OFF,

    /** The last meal (or any log) was so recent that waiting pushes the reminder past the slot. */
    ATE_RECENTLY,

    /** The slot is over for today. */
    WINDOW_OVER,
}

data class MealSlotPlan(
    val slot: MealSlot,
    val learned: LearnedMealTime,
    val decision: MealReminderDecision,
)
