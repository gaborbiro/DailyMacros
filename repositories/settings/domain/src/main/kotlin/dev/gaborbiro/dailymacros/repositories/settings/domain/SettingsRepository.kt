package dev.gaborbiro.dailymacros.repositories.settings.domain

import dev.gaborbiro.dailymacros.repositories.settings.domain.model.AutoSyncErrorStatus
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.BackupInterval
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.CloudSyncProvider
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptUsageStats
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

    /** Version number currently applied for the given prompt type; 0 means the defaults. */
    fun getActivePromptVersion(type: String): Int = 0

    fun setActivePromptVersion(type: String, version: Int) {}

    /** Records one AI query against the version of the given prompt type currently in effect. */
    fun recordPromptUsage(type: String, totalTokens: Long) {}

    /** Usage stats for the given prompt type, keyed by version number (0 = defaults). */
    fun getPromptUsageStats(type: String): Map<Int, PromptUsageStats> = emptyMap()

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

    fun getAutoPhotoRecognitionEnabled(): Boolean = false
    fun setAutoPhotoRecognitionEnabled(enabled: Boolean) {}

    /** Whether tapping a meal on the Quick Pick widget shows a confirmation dialog before logging. */
    fun getQuickPickConfirmationEnabled(): Boolean = true
    fun setQuickPickConfirmationEnabled(enabled: Boolean) {}

    fun getLastProcessedMediaStoreId(): Long = -1L
    fun setLastProcessedMediaStoreId(id: Long) {}

    /**
     * MediaStore ids of gallery photos the user has already attached to an entry by hand,
     * so auto photo recognition doesn't offer them again. Pruned once the photo monitor's
     * high-water mark passes them.
     */
    fun getManuallyAddedMediaStoreIds(): Set<Long> = emptySet()
    fun setManuallyAddedMediaStoreIds(ids: Set<Long>) {}

    fun getLastPhotoRecognitionRequestEpochMs(): Long = 0L
    fun setLastPhotoRecognitionRequestEpochMs(epochMs: Long) {}

    fun getLastBackupAttemptEpochMs(): Long? = null
    fun setLastBackupAttemptEpochMs(epochMs: Long) {}

    fun getAutoBackupInterval(): BackupInterval = BackupInterval.NEVER
    fun setAutoBackupInterval(interval: BackupInterval) {}

    fun getAutoSyncErrorStatus(): AutoSyncErrorStatus? = null
    fun setAutoSyncErrorStatus(status: AutoSyncErrorStatus?) {}
}
