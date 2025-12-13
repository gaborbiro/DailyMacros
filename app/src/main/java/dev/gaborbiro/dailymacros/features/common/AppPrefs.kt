package dev.gaborbiro.dailymacros.features.common

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class AppPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var showCoachMark: Boolean
        get() = prefs.getBoolean("show_coach_mark", true)
        set(value) = prefs.edit { putBoolean("show_coach_mark", value) }

    @OptIn(ExperimentalUuidApi::class)
    val userUUID: String
        get() {
            val existing = prefs.getString("user_uuid_3", null)
            if (existing != null) return existing

            val newUuid = ThreeWordId.random()
            prefs.edit { putString("user_uuid_3", newUuid) }
            return newUuid
        }

    // Remove value
    fun clear(key: String) {
        prefs.edit { remove(key) }
    }

    // Clear all
    fun clearAll() {
        prefs.edit { clear() }
    }
}
