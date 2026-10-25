package dev.gaborbiro.dailymacros.features.shared.mealreminders

/**
 * Which past diary days count as "logged" when learning someone's usual meal times. Mirrors the
 * Trends "which days count" setting (see TrendsPreferences.dayQualificationMode), so a day the
 * user excluded from their averages doesn't teach the reminders either.
 */
sealed interface LoggedDayRule {

    /**
     * Any day with at least one record. Covers both "all calendar days" and "only logged days":
     * for meal timing they're equivalent, since a day without records has no meal times to learn.
     */
    data object AnyRecord : LoggedDayRule

    /** Only days whose known calories add up to at least [threshold] (half-logged days are skipped). */
    data class MinimumCalories(val threshold: Long) : LoggedDayRule
}

/** Reads the user's current [LoggedDayRule]. Bound by the trends module, which owns the setting. */
fun interface LoggedDayRuleProvider {
    fun get(): LoggedDayRule
}
