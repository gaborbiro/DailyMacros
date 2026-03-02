package dev.gaborbiro.dailymacros

import android.content.Context
import androidx.core.content.edit
import dev.gaborbiro.dailymacros.features.common.ThreeWordId
import kotlin.uuid.ExperimentalUuidApi

internal class AppPrefs(context: Context) {

    companion object {
        private const val KEY_USER_UUID = "user_uuid_3"
    }

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

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