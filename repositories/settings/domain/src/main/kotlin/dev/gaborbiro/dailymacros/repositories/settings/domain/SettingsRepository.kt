package dev.gaborbiro.dailymacros.repositories.settings.domain

import dev.gaborbiro.dailymacros.repositories.settings.domain.model.CloudSyncProvider
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets

interface SettingsRepository {

    fun getTargets(): Targets

    fun setTargets(targets: Targets)

    /** Hour of day (0–23) when the food diary day rolls over; 0 means midnight. */
    fun getDiaryDayStartHour(): Int

    fun setDiaryDayStartHour(hourOfDay: Int)

    /** Returns user overrides for AI prompt editable segments, keyed by segment ID. */
    fun getPromptCustomisations(): Map<String, String>

    fun setPromptCustomisations(values: Map<String, String>)

    fun clearPromptCustomisations()

    fun getPromptVersions(type: String): List<PromptVersion>

    /** Creates a new version record for the given prompt type, persists it, and returns it. Does NOT update the active customisations. */
    fun savePromptVersion(type: String, customisations: Map<String, String>): PromptVersion

    /** Deletes the version with the given version number. */
    fun deletePromptVersion(version: Int)

    fun getApiKeyOverride(): String?
    fun setApiKeyOverride(key: String)
    fun clearApiKeyOverride()

    /** Returns stored customisations only when an API key override is active; emptyMap() otherwise. */
    fun getEffectiveCustomisations(): Map<String, String> =
        if (getApiKeyOverride() != null) getPromptCustomisations() else emptyMap()

    fun getCloudSyncProvider(): CloudSyncProvider = CloudSyncProvider.NONE
    fun setCloudSyncProvider(provider: CloudSyncProvider) {}
    fun getCloudSyncEmail(): String? = null
    fun setCloudSyncEmail(email: String?) {}
    fun getLastSyncedEpochMs(): Long? = null
    fun setLastSyncedEpochMs(epochMs: Long?) {}
}
