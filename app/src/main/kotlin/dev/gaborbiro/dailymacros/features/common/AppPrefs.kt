package dev.gaborbiro.dailymacros.features.common

import android.content.Context
import androidx.core.content.edit
import kotlin.uuid.ExperimentalUuidApi

internal class AppPrefs(context: Context) {

    companion object {
        private const val KEY_USER_UUID = "user_uuid_3"
        private const val KEY_SHOW_COACH_MARK = "show_coach_mark"
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
}
