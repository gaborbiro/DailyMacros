package dev.gaborbiro.dailymacros.repositories.settings

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository as SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.AutoSyncError
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.AutoSyncErrorStatus
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.BackupInterval
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.CloudSyncProvider
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptUsageStats
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.emptyMap

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val mapper: SettingsMapper,
) : SettingsRepository {

    private val prefs = context.getSharedPreferences("settings2", Context.MODE_PRIVATE)
    private val gson = Gson()

    override fun setTargets(targets: Targets) {
        val json = mapper.map(targets)
        prefs.edit {
            putString(KEY_TARGETS, json)
        }
    }

    private val defaultTargets = Targets(
        calories = Target(enabled = false),
        protein = Target(enabled = false),
        salt = Target(enabled = false),
        fibre = Target(enabled = false),
        fat = Target(enabled = false),
        ofWhichSaturated = Target(enabled = false),
        carbs = Target(enabled = false),
        ofWhichSugar = Target(enabled = false)
    )

    override fun getTargets(): Targets {
        val json = prefs.getString(KEY_TARGETS, null) ?: return defaultTargets
        return mapper.map(json)
    }

    override fun getDiaryDayStartHour(): Int =
        prefs.getInt(KEY_DIARY_DAY_START_HOUR, DEFAULT_DIARY_DAY_START_HOUR)
            .coerceIn(0, 23)

    override fun setDiaryDayStartHour(hourOfDay: Int) {
        prefs.edit {
            putInt(KEY_DIARY_DAY_START_HOUR, hourOfDay.coerceIn(0, 23))
        }
    }

    override fun getPromptCustomisations(): Map<String, String> {
        val json = prefs.getString(KEY_PROMPT_CUSTOMISATIONS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, String>>() {}.type
        return runCatching { gson.fromJson<Map<String, String>>(json, type) }.getOrDefault(emptyMap())
    }

    override fun setPromptCustomisations(values: Map<String, String>) {
        prefs.edit {
            putString(KEY_PROMPT_CUSTOMISATIONS, gson.toJson(values))
        }
    }

    override fun clearPromptCustomisations() {
        prefs.edit {
            putString(KEY_PROMPT_CUSTOMISATIONS, gson.toJson(emptyMap<String, String>()))
            remove(KEY_ACTIVE_PROMPT_VERSIONS)
        }
    }

    private fun getAllPromptVersions(): List<PromptVersion> {
        val json = prefs.getString(KEY_PROMPT_VERSIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<PromptVersion>>() {}.type
        return runCatching { gson.fromJson<List<PromptVersion>>(json, type) }.getOrDefault(emptyList())
    }

    override fun getPromptVersions(type: String): List<PromptVersion> =
        getAllPromptVersions().filter { it.type == type }

    override fun deletePromptVersion(version: Int) {
        val updated = getAllPromptVersions().filter { it.version != version }
        val remainingStats = getAllPromptUsageStats()
            .filterKeys { it.substringAfterLast(':').toIntOrNull() != version }
        prefs.edit {
            putString(KEY_PROMPT_VERSIONS, gson.toJson(updated))
            putString(KEY_PROMPT_USAGE_STATS, gson.toJson(remainingStats))
        }
    }

    override fun savePromptVersion(type: String, customisations: Map<String, String>): PromptVersion {
        val existing = getAllPromptVersions()
        val nextVersion = (existing.maxOfOrNull { it.version } ?: 0) + 1
        val newVersion = PromptVersion(
            version = nextVersion,
            createdAt = System.currentTimeMillis(),
            customisations = customisations,
            type = type,
        )
        prefs.edit { putString(KEY_PROMPT_VERSIONS, gson.toJson(existing + newVersion)) }
        return newVersion
    }

    private fun getActivePromptVersions(): Map<String, Int> {
        val json = prefs.getString(KEY_ACTIVE_PROMPT_VERSIONS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, Int>>() {}.type
        return runCatching { gson.fromJson<Map<String, Int>>(json, type) }.getOrDefault(emptyMap())
    }

    override fun getActivePromptVersion(type: String): Int = getActivePromptVersions()[type] ?: 0

    override fun setActivePromptVersion(type: String, version: Int) {
        val updated = getActivePromptVersions() + (type to version)
        prefs.edit { putString(KEY_ACTIVE_PROMPT_VERSIONS, gson.toJson(updated)) }
    }

    /** Keyed by "<promptType>:<versionNumber>". */
    private fun getAllPromptUsageStats(): Map<String, PromptUsageStats> {
        val json = prefs.getString(KEY_PROMPT_USAGE_STATS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, PromptUsageStats>>() {}.type
        return runCatching { gson.fromJson<Map<String, PromptUsageStats>>(json, type) }.getOrDefault(emptyMap())
    }

    override fun recordPromptUsage(type: String, totalTokens: Long) {
        // Customisations only take effect with an API key override; without one the defaults (v0) are in use
        val version = if (getApiKeyOverride() != null) getActivePromptVersion(type) else 0
        val all = getAllPromptUsageStats().toMutableMap()
        val key = "$type:$version"
        val existing = all[key] ?: PromptUsageStats(count = 0, totalTokens = 0L)
        all[key] = PromptUsageStats(
            count = existing.count + 1,
            totalTokens = existing.totalTokens + totalTokens,
        )
        prefs.edit { putString(KEY_PROMPT_USAGE_STATS, gson.toJson(all)) }
    }

    override fun getPromptUsageStats(type: String): Map<Int, PromptUsageStats> =
        getAllPromptUsageStats().entries.mapNotNull { (key, stats) ->
            val keyType = key.substringBeforeLast(':')
            val version = key.substringAfterLast(':').toIntOrNull()
            if (keyType == type && version != null) version to stats else null
        }.toMap()

    override fun getApiKeyOverride(): String? = prefs.getString(KEY_API_KEY_OVERRIDE, null)?.takeIf { it.isNotBlank() }

    override fun setApiKeyOverride(key: String) {
        prefs.edit { putString(KEY_API_KEY_OVERRIDE, key) }
    }

    override fun clearApiKeyOverride() {
        prefs.edit { remove(KEY_API_KEY_OVERRIDE) }
    }

    override fun getCloudSyncProvider(): CloudSyncProvider =
        prefs.getString(KEY_CLOUD_SYNC_PROVIDER, null)
            ?.let { runCatching { CloudSyncProvider.valueOf(it) }.getOrNull() }
            ?: CloudSyncProvider.NONE

    override fun setCloudSyncProvider(provider: CloudSyncProvider) {
        prefs.edit { putString(KEY_CLOUD_SYNC_PROVIDER, provider.name) }
    }

    override fun getCloudSyncEmail(): String? = prefs.getString(KEY_CLOUD_SYNC_EMAIL, null)

    override fun setCloudSyncEmail(email: String?) {
        prefs.edit { if (email != null) putString(KEY_CLOUD_SYNC_EMAIL, email) else remove(KEY_CLOUD_SYNC_EMAIL) }
    }

    override fun getLastSyncedEpochMs(): Long? =
        prefs.getLong(KEY_LAST_SYNCED_EPOCH_MS, -1L).takeIf { it >= 0 }

    override fun setLastSyncedEpochMs(epochMs: Long?) {
        prefs.edit { if (epochMs != null) putLong(KEY_LAST_SYNCED_EPOCH_MS, epochMs) else remove(KEY_LAST_SYNCED_EPOCH_MS) }
    }

    override fun getAutoPhotoRecognitionEnabled(): Boolean =
        prefs.getBoolean(KEY_AUTO_PHOTO_RECOGNITION, false)

    override fun setAutoPhotoRecognitionEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_PHOTO_RECOGNITION, enabled) }
    }

    override fun getQuickPickConfirmationEnabled(): Boolean =
        prefs.getBoolean(KEY_QUICK_PICK_CONFIRMATION, true)

    override fun setQuickPickConfirmationEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_QUICK_PICK_CONFIRMATION, enabled) }
    }

    override fun getLastProcessedMediaStoreId(): Long =
        prefs.getLong(KEY_LAST_PROCESSED_MEDIA_STORE_ID, -1L)

    override fun setLastProcessedMediaStoreId(id: Long) {
        prefs.edit { putLong(KEY_LAST_PROCESSED_MEDIA_STORE_ID, id) }
    }

    override fun getManuallyAddedMediaStoreIds(): Set<Long> =
        prefs.getStringSet(KEY_MANUALLY_ADDED_MEDIA_STORE_IDS, emptySet())
            .orEmpty()
            .mapNotNullTo(mutableSetOf()) { it.toLongOrNull() }

    override fun setManuallyAddedMediaStoreIds(ids: Set<Long>) {
        prefs.edit {
            putStringSet(KEY_MANUALLY_ADDED_MEDIA_STORE_IDS, ids.mapTo(mutableSetOf()) { it.toString() })
        }
    }

    override fun getLastPhotoRecognitionRequestEpochMs(): Long =
        prefs.getLong(KEY_LAST_PHOTO_RECOGNITION_REQUEST_EPOCH_MS, 0L)

    override fun setLastPhotoRecognitionRequestEpochMs(epochMs: Long) {
        prefs.edit { putLong(KEY_LAST_PHOTO_RECOGNITION_REQUEST_EPOCH_MS, epochMs) }
    }

    override fun getLastBackupAttemptEpochMs(): Long? =
        prefs.getLong(KEY_LAST_BACKUP_ATTEMPT_EPOCH_MS, -1L).takeIf { it >= 0 }

    override fun setLastBackupAttemptEpochMs(epochMs: Long) {
        prefs.edit { putLong(KEY_LAST_BACKUP_ATTEMPT_EPOCH_MS, epochMs) }
    }

    override fun getAutoBackupInterval(): BackupInterval =
        prefs.getString(KEY_AUTO_BACKUP_INTERVAL, null)
            ?.let { runCatching { BackupInterval.valueOf(it) }.getOrNull() }
            ?: BackupInterval.NEVER

    override fun setAutoBackupInterval(interval: BackupInterval) {
        prefs.edit { putString(KEY_AUTO_BACKUP_INTERVAL, interval.name) }
    }

    override fun getAutoSyncErrorStatus(): AutoSyncErrorStatus? {
        val error = prefs.getString(KEY_AUTO_SYNC_ERROR, null)
            ?.let { runCatching { AutoSyncError.valueOf(it) }.getOrNull() }
            ?: return null
        val notifiedAt = prefs.getLong(KEY_AUTO_SYNC_ERROR_NOTIFIED_EPOCH_MS, -1L).takeIf { it >= 0 }
            ?: return null
        return AutoSyncErrorStatus(error = error, notifiedEpochMs = notifiedAt)
    }

    override fun setAutoSyncErrorStatus(status: AutoSyncErrorStatus?) {
        prefs.edit {
            if (status != null) {
                putString(KEY_AUTO_SYNC_ERROR, status.error.name)
                putLong(KEY_AUTO_SYNC_ERROR_NOTIFIED_EPOCH_MS, status.notifiedEpochMs)
            } else {
                remove(KEY_AUTO_SYNC_ERROR)
                remove(KEY_AUTO_SYNC_ERROR_NOTIFIED_EPOCH_MS)
            }
        }
    }

    companion object {
        private const val KEY_TARGETS = "targets_json"
        private const val KEY_DIARY_DAY_START_HOUR = "diary_day_start_hour"
        private const val DEFAULT_DIARY_DAY_START_HOUR = 0
        private const val KEY_PROMPT_CUSTOMISATIONS = "prompt_customisations_json"
        private const val KEY_PROMPT_VERSIONS = "prompt_versions_json"
        private const val KEY_ACTIVE_PROMPT_VERSIONS = "active_prompt_versions_json"
        private const val KEY_PROMPT_USAGE_STATS = "prompt_usage_stats_json"
        private const val KEY_API_KEY_OVERRIDE = "api_key_override"
        private const val KEY_CLOUD_SYNC_PROVIDER = "cloud_sync_provider"
        private const val KEY_CLOUD_SYNC_EMAIL = "cloud_sync_email"
        private const val KEY_LAST_SYNCED_EPOCH_MS = "last_synced_epoch_ms"
        private const val KEY_AUTO_PHOTO_RECOGNITION = "auto_photo_recognition"
        private const val KEY_QUICK_PICK_CONFIRMATION = "quick_pick_confirmation_enabled"
        private const val KEY_LAST_PROCESSED_MEDIA_STORE_ID = "last_processed_media_store_id"
        private const val KEY_MANUALLY_ADDED_MEDIA_STORE_IDS = "manually_added_media_store_ids"
        private const val KEY_LAST_PHOTO_RECOGNITION_REQUEST_EPOCH_MS = "last_photo_recognition_request_epoch_ms"
        private const val KEY_LAST_BACKUP_ATTEMPT_EPOCH_MS = "last_backup_attempt_epoch_ms"
        private const val KEY_AUTO_BACKUP_INTERVAL = "auto_backup_interval"
        private const val KEY_AUTO_SYNC_ERROR = "auto_sync_error"
        private const val KEY_AUTO_SYNC_ERROR_NOTIFIED_EPOCH_MS = "auto_sync_error_notified_epoch_ms"
    }
}
