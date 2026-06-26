package dev.gaborbiro.dailymacros.features.trends

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject

interface TrendsPreferences {

    var dayQualificationMode: String

    var qualifyingCalorieThreshold: Long

    var insights: Map<String, String>

    var insightsDateRange: String?

    var ongoingInsights: Map<String, String>

    var ongoingInsightsDateRange: String?

    companion object {
        const val MODE_ALL_CALENDAR_DAYS = "aggregation_mode_calendar_days"
        const val MODE_ONLY_LOGGED_DAYS = "aggregation_mode_logged_days"
        const val MODE_ONLY_QUALIFIED_DAYS = "aggregation_mode_qualified_days"
    }
}

class TrendsPreferencesImpl @Inject constructor(
    @ApplicationContext context: Context,
) : TrendsPreferences {

    private val prefs = context.applicationContext.getSharedPreferences("trends_prefs", Context.MODE_PRIVATE)

    override var dayQualificationMode: String
        get() = prefs.getString(KEY_DAY_QUALIFICATION_MODE, TrendsPreferences.MODE_ALL_CALENDAR_DAYS)
            ?: TrendsPreferences.MODE_ALL_CALENDAR_DAYS
        set(value) = prefs.edit { putString(KEY_DAY_QUALIFICATION_MODE, value) }

    override var qualifyingCalorieThreshold: Long
        get() = prefs.getLong(KEY_QUALIFYING_CALORIE_THRESHOLD, DEFAULT_QUALIFYING_THRESHOLD)
        set(value) = prefs.edit { putLong(KEY_QUALIFYING_CALORIE_THRESHOLD, value) }

    override var insights: Map<String, String>
        get() {
            val json = prefs.getString(KEY_INSIGHTS, null) ?: return emptyMap()
            return try {
                val obj = JSONObject(json)
                obj.keys().asSequence().associateWith { obj.getString(it) }
            } catch (_: Exception) { emptyMap() }
        }
        set(value) = prefs.edit { putString(KEY_INSIGHTS, JSONObject(value as Map<*, *>).toString()) }

    override var insightsDateRange: String?
        get() = prefs.getString(KEY_INSIGHTS_DATE_RANGE, null)
        set(value) = prefs.edit {
            if (value != null) putString(KEY_INSIGHTS_DATE_RANGE, value) else remove(KEY_INSIGHTS_DATE_RANGE)
        }

    override var ongoingInsights: Map<String, String>
        get() {
            val json = prefs.getString(KEY_ONGOING_INSIGHTS, null) ?: return emptyMap()
            return try {
                val obj = JSONObject(json)
                obj.keys().asSequence().associateWith { obj.getString(it) }
            } catch (_: Exception) { emptyMap() }
        }
        set(value) = prefs.edit { putString(KEY_ONGOING_INSIGHTS, JSONObject(value as Map<*, *>).toString()) }

    override var ongoingInsightsDateRange: String?
        get() = prefs.getString(KEY_ONGOING_INSIGHTS_DATE_RANGE, null)
        set(value) = prefs.edit {
            if (value != null) putString(KEY_ONGOING_INSIGHTS_DATE_RANGE, value) else remove(KEY_ONGOING_INSIGHTS_DATE_RANGE)
        }

    private companion object {
        const val KEY_DAY_QUALIFICATION_MODE = "aggregation_mode"
        const val KEY_QUALIFYING_CALORIE_THRESHOLD = "qualified_aggregation_threshold"
        const val KEY_INSIGHTS = "weekly_insights"
        const val KEY_INSIGHTS_DATE_RANGE = "weekly_insights_date_range"
        const val KEY_ONGOING_INSIGHTS = "ongoing_insights"
        const val KEY_ONGOING_INSIGHTS_DATE_RANGE = "ongoing_insights_date_range"
        const val DEFAULT_QUALIFYING_THRESHOLD = 800L
    }
}
