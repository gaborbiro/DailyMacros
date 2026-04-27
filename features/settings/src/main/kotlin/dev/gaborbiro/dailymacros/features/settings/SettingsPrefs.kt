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

    /** Epoch millis when request/response JSON was last fetched; 0 if never saved. */
    var variabilityMiningGeneratedAtEpochMs: Long
        get() = prefs.getLong(KEY_VARIABILITY_MINING_GENERATED_AT_EPOCH_MS, 0L)
        set(value) = prefs.edit { putLong(KEY_VARIABILITY_MINING_GENERATED_AT_EPOCH_MS, value) }

    /**
     * One '0'/'1' per preorder JSON container node for the saved request JSON viewer; empty = all collapsed.
     */
    var variabilityMiningRequestJsonExpansionBits: String
        get() = prefs.getString(KEY_VARIABILITY_MINING_REQUEST_JSON_EXPANSION_BITS, "") ?: ""
        set(value) = prefs.edit { putString(KEY_VARIABILITY_MINING_REQUEST_JSON_EXPANSION_BITS, value) }

    /** Same as [variabilityMiningRequestJsonExpansionBits] for the response JSON viewer. */
    var variabilityMiningResponseJsonExpansionBits: String
        get() = prefs.getString(KEY_VARIABILITY_MINING_RESPONSE_JSON_EXPANSION_BITS, "") ?: ""
        set(value) = prefs.edit { putString(KEY_VARIABILITY_MINING_RESPONSE_JSON_EXPANSION_BITS, value) }

    /** Whether the settings Request JSON collapsible block is open. */
    var variabilityMiningRequestJsonSectionExpanded: Boolean
        get() = prefs.getBoolean(KEY_VARIABILITY_MINING_REQUEST_JSON_SECTION_EXPANDED, false)
        set(value) = prefs.edit { putBoolean(KEY_VARIABILITY_MINING_REQUEST_JSON_SECTION_EXPANDED, value) }

    /** Whether the settings Response JSON collapsible block is open. */
    var variabilityMiningResponseJsonSectionExpanded: Boolean
        get() = prefs.getBoolean(KEY_VARIABILITY_MINING_RESPONSE_JSON_SECTION_EXPANDED, false)
        set(value) = prefs.edit { putBoolean(KEY_VARIABILITY_MINING_RESPONSE_JSON_SECTION_EXPANDED, value) }

    fun clearVariabilityMiningDebugCache() {
        prefs.edit {
            remove(KEY_VARIABILITY_MINING_REQUEST_JSON)
            remove(KEY_VARIABILITY_MINING_RESPONSE_JSON)
            putLong(KEY_VARIABILITY_MINING_GENERATED_AT_EPOCH_MS, 0L)
            putString(KEY_VARIABILITY_MINING_REQUEST_JSON_EXPANSION_BITS, "")
            putString(KEY_VARIABILITY_MINING_RESPONSE_JSON_EXPANSION_BITS, "")
            putBoolean(KEY_VARIABILITY_MINING_REQUEST_JSON_SECTION_EXPANDED, false)
            putBoolean(KEY_VARIABILITY_MINING_RESPONSE_JSON_SECTION_EXPANDED, false)
        }
    }

    private companion object {
        private const val KEY_VARIABILITY_MINING_REQUEST_JSON = "variability_mining_request_json_1"
        private const val KEY_VARIABILITY_MINING_RESPONSE_JSON = "variability_mining_response_json_1"
        private const val KEY_VARIABILITY_MINING_GENERATED_AT_EPOCH_MS = "variability_mining_generated_at_epoch_ms_1"
        private const val KEY_VARIABILITY_MINING_REQUEST_JSON_EXPANSION_BITS =
            "variability_mining_request_json_expansion_bits_1"
        private const val KEY_VARIABILITY_MINING_RESPONSE_JSON_EXPANSION_BITS =
            "variability_mining_response_json_expansion_bits_1"
        private const val KEY_VARIABILITY_MINING_REQUEST_JSON_SECTION_EXPANDED =
            "variability_mining_request_json_section_expanded_1"
        private const val KEY_VARIABILITY_MINING_RESPONSE_JSON_SECTION_EXPANDED =
            "variability_mining_response_json_section_expanded_1"
    }
}
