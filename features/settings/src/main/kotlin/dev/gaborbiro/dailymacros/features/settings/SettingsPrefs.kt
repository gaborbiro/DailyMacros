package dev.gaborbiro.dailymacros.features.settings

import android.content.Context
import androidx.core.content.edit

class SettingsPrefs(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    var variabilityMiningRequestJson: String?
        get() = prefs.getString(KEY_VARIABILITY_MINING_REQUEST_JSON, null)
        set(value) = prefs.edit { putString(KEY_VARIABILITY_MINING_REQUEST_JSON, value) }

    var variabilityMiningResponseJson: String?
        get() = prefs.getString(KEY_VARIABILITY_MINING_RESPONSE_JSON, null)
        set(value) = prefs.edit { putString(KEY_VARIABILITY_MINING_RESPONSE_JSON, value) }

    private companion object {
        private const val KEY_VARIABILITY_MINING_REQUEST_JSON = "variability_mining_request_json_1"
        private const val KEY_VARIABILITY_MINING_RESPONSE_JSON = "variability_mining_response_json_1"
    }
}
