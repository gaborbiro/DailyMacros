package dev.gaborbiro.dailymacros.features.trends

import android.content.Context
import androidx.core.content.edit

/**
 * Trends-specific preferences (day qualification mode and calorie threshold).
 * Use [TrendsPreferencesImpl] for a Context-based implementation.
 */
interface TrendsPreferences {

    var dayQualificationMode: String

    var qualifyingCalorieThreshold: Long

    companion object {
        const val MODE_ALL_CALENDAR_DAYS = "aggregation_mode_calendar_days"
        const val MODE_ONLY_LOGGED_DAYS = "aggregation_mode_logged_days"
        const val MODE_ONLY_QUALIFIED_DAYS = "aggregation_mode_qualified_days"
    }
}

/**
 * Default implementation using SharedPreferences (file "trends_prefs").
 */
class TrendsPreferencesImpl(
    context: Context,
) : TrendsPreferences {

    private val prefs = context.applicationContext.getSharedPreferences("trends_prefs", Context.MODE_PRIVATE)

    override var dayQualificationMode: String
        get() = prefs.getString(KEY_DAY_QUALIFICATION_MODE, TrendsPreferences.MODE_ALL_CALENDAR_DAYS)
            ?: TrendsPreferences.MODE_ALL_CALENDAR_DAYS
        set(value) = prefs.edit { putString(KEY_DAY_QUALIFICATION_MODE, value) }

    override var qualifyingCalorieThreshold: Long
        get() = prefs.getLong(KEY_QUALIFYING_CALORIE_THRESHOLD, DEFAULT_QUALIFYING_THRESHOLD)
        set(value) = prefs.edit { putLong(KEY_QUALIFYING_CALORIE_THRESHOLD, value) }

    private companion object {
        const val KEY_DAY_QUALIFICATION_MODE = "aggregation_mode"
        const val KEY_QUALIFYING_CALORIE_THRESHOLD = "qualified_aggregation_threshold"
        const val DEFAULT_QUALIFYING_THRESHOLD = 800L
    }
}
