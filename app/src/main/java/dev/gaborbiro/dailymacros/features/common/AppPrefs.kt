package dev.gaborbiro.dailymacros.features.common

import android.content.Context
import androidx.annotation.StringDef
import androidx.core.content.edit
import kotlin.uuid.ExperimentalUuidApi

internal class AppPrefs(context: Context) {

    companion object {
        const val AGGREGATION_MODE_CALENDAR_DAYS = "aggregation_mode_calendar_days"
        const val AGGREGATION_MODE_LOGGED_DAYS = "aggregation_mode_logged_days"
        const val AGGREGATION_MODE_QUALIFIED_DAYS = "aggregation_mode_qualified_days"

        @StringDef(
            AGGREGATION_MODE_CALENDAR_DAYS,
            AGGREGATION_MODE_LOGGED_DAYS,
            AGGREGATION_MODE_QUALIFIED_DAYS,
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class AggregationMode

        private const val KEY_AGGREGATION_MODE = "aggregation_mode"
        private const val KEY_USER_UUID = "user_uuid_3"
        private const val KEY_SHOW_COACH_MARK = "show_coach_mark"
        private const val KEY_QUALIFIED_AGGREGATION_THRESHOLD = "qualified_aggregation_threshold"
    }

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var showCoachMark: Boolean
        get() = prefs.getBoolean(KEY_SHOW_COACH_MARK, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_COACH_MARK, value) }

    @OptIn(ExperimentalUuidApi::class)
    val userUUID: String
        get() {
            val existing = prefs.getString(KEY_USER_UUID, null)
            if (existing != null) return existing

            val newUuid = ThreeWordId.random()
            prefs.edit { putString(KEY_USER_UUID, newUuid) }
            return newUuid
        }

    var aggregationMode: String
        @AggregationMode get() = prefs.getString(KEY_AGGREGATION_MODE, AGGREGATION_MODE_CALENDAR_DAYS)!!
        set(@AggregationMode value) = prefs.edit { putString(KEY_AGGREGATION_MODE, value) }

    var qualifiedAggregationThreshold: Long
        get() = prefs.getLong(KEY_QUALIFIED_AGGREGATION_THRESHOLD, 800L)
        set(value) = prefs.edit { putLong(KEY_QUALIFIED_AGGREGATION_THRESHOLD, value) }

    fun clear(key: String) {
        prefs.edit { remove(key) }
    }

    fun clearAll() {
        prefs.edit { clear() }
    }
}
