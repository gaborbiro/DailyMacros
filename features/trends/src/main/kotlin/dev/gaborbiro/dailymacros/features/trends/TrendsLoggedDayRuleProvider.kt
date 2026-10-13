package dev.gaborbiro.dailymacros.features.trends

import dev.gaborbiro.dailymacros.features.shared.mealreminders.LoggedDayRule
import dev.gaborbiro.dailymacros.features.shared.mealreminders.LoggedDayRuleProvider
import javax.inject.Inject

/** Exposes the Trends "which days count" setting to the meal reminders, so both agree on what a logged day is. */
class TrendsLoggedDayRuleProvider @Inject constructor(
    private val preferences: TrendsPreferences,
) : LoggedDayRuleProvider {

    override fun get(): LoggedDayRule = when (preferences.dayQualificationMode) {
        TrendsPreferences.MODE_ONLY_QUALIFIED_DAYS ->
            LoggedDayRule.MinimumCalories(preferences.qualifyingCalorieThreshold)

        else -> LoggedDayRule.AnyRecord
    }
}
