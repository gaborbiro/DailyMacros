package dev.gaborbiro.dailymacros.features.trends

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject

interface TrendsPreferences {

    var dayQualificationMode: String

    var qualifyingCalorieThreshold: Long

    var weeklyInsights: Map<String, String>

    var weeklyInsightsDateRange: String?

    var ongoingWeekInsights: String?

    var ongoingInsightsDateRange: String?

    var weeklyInsightsWeekAssessment: String?

    var weeklyInsightsFetchedAt: Long?

    var ongoingInsightsFetchedAt: Long?

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

    override var weeklyInsights: Map<String, String>
        get() {
            val json = prefs.getString(KEY_WEEKLY_INSIGHTS, null) ?: return emptyMap()
            return try {
                val obj = JSONObject(json)
                obj.keys().asSequence().associateWith { obj.getString(it) }
            } catch (_: Exception) { emptyMap() }
        }
        set(value) = prefs.edit { putString(KEY_WEEKLY_INSIGHTS, JSONObject(value as Map<*, *>).toString()) }

    override var weeklyInsightsDateRange: String?
        get() = prefs.getString(KEY_WEEKLY_INSIGHTS_DATE_RANGE, null)
        set(value) = prefs.edit {
            if (value != null) putString(KEY_WEEKLY_INSIGHTS_DATE_RANGE, value) else remove(KEY_WEEKLY_INSIGHTS_DATE_RANGE)
        }

    override var ongoingWeekInsights: String?
        get() = prefs.getString(KEY_ONGOING_WEEK_INSIGHTS, null)
        set(value) = prefs.edit { putString(KEY_ONGOING_WEEK_INSIGHTS, value) }

    override var ongoingInsightsDateRange: String?
        get() = prefs.getString(KEY_ONGOING_WEEK_INSIGHTS_DATE_RANGE, null)
        set(value) = prefs.edit {
            if (value != null) putString(KEY_ONGOING_WEEK_INSIGHTS_DATE_RANGE, value) else remove(KEY_ONGOING_WEEK_INSIGHTS_DATE_RANGE)
        }

    override var weeklyInsightsWeekAssessment: String?
        get() = prefs.getString(KEY_WEEKLY_INSIGHTS_WEEK_ASSESSMENT, null)
        set(value) = prefs.edit {
            if (value != null) putString(KEY_WEEKLY_INSIGHTS_WEEK_ASSESSMENT, value) else remove(KEY_WEEKLY_INSIGHTS_WEEK_ASSESSMENT)
        }

    override var weeklyInsightsFetchedAt: Long?
        get() = prefs.getLong(KEY_WEEKLY_INSIGHTS_FETCHED_AT, -1L).takeIf { it >= 0 }
        set(value) = prefs.edit {
            if (value != null) putLong(KEY_WEEKLY_INSIGHTS_FETCHED_AT, value) else remove(KEY_WEEKLY_INSIGHTS_FETCHED_AT)
        }

    override var ongoingInsightsFetchedAt: Long?
        get() = prefs.getLong(KEY_ONGOING_INSIGHTS_FETCHED_AT, -1L).takeIf { it >= 0 }
        set(value) = prefs.edit {
            if (value != null) putLong(KEY_ONGOING_INSIGHTS_FETCHED_AT, value) else remove(KEY_ONGOING_INSIGHTS_FETCHED_AT)
        }

    private companion object {
        const val KEY_DAY_QUALIFICATION_MODE = "aggregation_mode"
        const val KEY_QUALIFYING_CALORIE_THRESHOLD = "qualified_aggregation_threshold"
        const val KEY_WEEKLY_INSIGHTS = "weekly_insights"
        const val KEY_WEEKLY_INSIGHTS_DATE_RANGE = "weekly_insights_date_range"
        const val KEY_ONGOING_WEEK_INSIGHTS = "ongoing_week_insights"
        const val KEY_ONGOING_WEEK_INSIGHTS_DATE_RANGE = "ongoing_week_insights_date_range"
        const val KEY_WEEKLY_INSIGHTS_WEEK_ASSESSMENT = "weekly_insights_week_assessment"
        const val KEY_WEEKLY_INSIGHTS_FETCHED_AT = "weekly_insights_fetched_at"
        const val KEY_ONGOING_INSIGHTS_FETCHED_AT = "ongoing_insights_fetched_at"
        const val DEFAULT_QUALIFYING_THRESHOLD = 800L
    }
}
