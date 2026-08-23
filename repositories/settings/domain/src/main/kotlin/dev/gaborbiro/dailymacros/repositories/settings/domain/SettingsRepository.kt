package dev.gaborbiro.dailymacros.repositories.settings.domain

import dev.gaborbiro.dailymacros.repositories.settings.domain.model.AutoSyncErrorStatus
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.BackupInterval
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.CloudSyncProvider
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PdfExportOptions
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptUsageStats
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.TimezoneEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface SettingsRepository {

    fun getTargets(): Targets

    fun setTargets(targets: Targets)

    /** Reactive equivalent of [getTargets] - emits the current value and again whenever it changes. */
    fun observeTargets(): Flow<Targets> = flowOf(getTargets())

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

    /** Whether tapping a Quick Pick, in the Quick Pick widget or in the main widget, shows a confirmation dialog before logging. */
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

    /** Whether automatic (non-user-initiated) backup sync should be skipped unless on Wi-Fi. */
    fun getWifiOnlyBackupEnabled(): Boolean = false
    fun setWifiOnlyBackupEnabled(enabled: Boolean) {}

    /** Whether automatic (non-user-initiated) macro analysis retries should wait for Wi-Fi. */
    fun getWifiOnlyAnalysisEnabled(): Boolean = false
    fun setWifiOnlyAnalysisEnabled(enabled: Boolean) {}

    fun getAutoSyncErrorStatus(): AutoSyncErrorStatus? = null
    fun setAutoSyncErrorStatus(status: AutoSyncErrorStatus?) {}

    /** Last content options chosen for the PDF food-diary export. */
    fun getPdfExportOptions(): PdfExportOptions = PdfExportOptions()
    fun setPdfExportOptions(options: PdfExportOptions) {}

    /**
     * Whether the user has dismissed Overview's proactive "subscribe" banner (shown once
     * they have at least one record and aren't subscribed yet). Sticky until subscribed -
     * dismissing it doesn't mean "never again", so it isn't wired to reappear on its own;
     * the notification/paywall entry points remain reachable regardless.
     */
    fun getSubscribeBannerDismissed(): Boolean = false
    fun setSubscribeBannerDismissed(dismissed: Boolean) {}

    /** Recent device timezone-change events, oldest first, pruned to the last 14 days. */
    fun getRecentTimezoneEvents(): List<TimezoneEvent> = emptyList()

    /** Appends a timezone-change event and prunes anything older than 14 days from [epochMs].
     *  A no-op if [zoneId] equals the most recently stored zone (the OS can refire redundantly). */
    fun recordTimezoneEvent(zoneId: String, epochMs: Long) {}
}
