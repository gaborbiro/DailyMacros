package dev.gaborbiro.dailymacros.features.common

import android.content.Context
import androidx.annotation.StringDef
import androidx.core.content.edit
import kotlin.uuid.ExperimentalUuidApi

internal class AppPrefs(context: Context) {

    companion object {
        const val DAY_QUALIFICATION_MODE_ALL_CALENDAR_DAYS = "aggregation_mode_calendar_days"
        const val DAY_QUALIFICATION_MODE_ONLY_LOGGED_DAYS = "aggregation_mode_logged_days"
        const val DAY_QUALIFICATION_MODE_ONLY_QUALIFIED_DAYS = "aggregation_mode_qualified_days"

        @StringDef(
            DAY_QUALIFICATION_MODE_ALL_CALENDAR_DAYS,
            DAY_QUALIFICATION_MODE_ONLY_LOGGED_DAYS,
            DAY_QUALIFICATION_MODE_ONLY_QUALIFIED_DAYS,
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class DayQualificationMode

        private const val KEY_DAY_QUALIFICATION_MODE = "aggregation_mode"
        private const val KEY_USER_UUID = "user_uuid_3"
        private const val KEY_SHOW_COACH_MARK = "show_coach_mark"
        private const val KEY_QUALIFYING_CALORIE_THRESHOLD = "qualified_aggregation_threshold"
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

    var dayQualificationMode: String
        @DayQualificationMode get() = prefs.getString(KEY_DAY_QUALIFICATION_MODE, DAY_QUALIFICATION_MODE_ALL_CALENDAR_DAYS)!!
        set(@DayQualificationMode value) = prefs.edit { putString(KEY_DAY_QUALIFICATION_MODE, value) }

    var qualifyingCalorieThreshold: Long
        get() = prefs.getLong(KEY_QUALIFYING_CALORIE_THRESHOLD, 800L)
        set(value) = prefs.edit { putLong(KEY_QUALIFYING_CALORIE_THRESHOLD, value) }

    fun clear(key: String) {
        prefs.edit { remove(key) }
    }

    fun clearAll() {
        prefs.edit { clear() }
    }
}
