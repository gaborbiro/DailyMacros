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

//    // String
//    var username: String?
//        get() = prefs.getString("username", null)
//        set(value) = prefs.edit().putString("username", value).apply()
//
//    // Int
//    var launchCount: Int
//        get() = prefs.getInt("launch_count", 0)
//        set(value) = prefs.edit().putInt("launch_count", value).apply()
//
//    // Float
//    var volume: Float
//        get() = prefs.getFloat("volume", 1.0f)
//        set(value) = prefs.edit().putFloat("volume", value).apply()

    // Remove value
    fun clear(key: String) {
        prefs.edit { remove(key) }
    }

    // Clear all
    fun clearAll() {
        prefs.edit { clear() }
    }
}
