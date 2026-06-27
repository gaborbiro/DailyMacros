package dev.gaborbiro.dailymacros.repositories.settings

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository as SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.CloudSyncProvider
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

    override fun getPromptCustomizations(): Map<String, String> {
        val json = prefs.getString(KEY_PROMPT_CUSTOMIZATIONS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, String>>() {}.type
        return runCatching { gson.fromJson<Map<String, String>>(json, type) }.getOrDefault(emptyMap())
    }

    override fun setPromptCustomizations(values: Map<String, String>) {
        prefs.edit {
            putString(KEY_PROMPT_CUSTOMIZATIONS, gson.toJson(values))
        }
    }

    override fun clearPromptCustomizations() {
        prefs.edit {
            putString(KEY_PROMPT_CUSTOMIZATIONS, gson.toJson(emptyMap<String, String>()))
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
        prefs.edit { putString(KEY_PROMPT_VERSIONS, gson.toJson(updated)) }
    }

    override fun savePromptVersion(type: String, customizations: Map<String, String>): PromptVersion {
        val existing = getAllPromptVersions()
        val nextVersion = (existing.maxOfOrNull { it.version } ?: 0) + 1
        val newVersion = PromptVersion(
            version = nextVersion,
            createdAt = System.currentTimeMillis(),
            customizations = customizations,
            type = type,
        )
        prefs.edit { putString(KEY_PROMPT_VERSIONS, gson.toJson(existing + newVersion)) }
        return newVersion
    }

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

    companion object {
        private const val KEY_TARGETS = "targets_json"
        private const val KEY_DIARY_DAY_START_HOUR = "diary_day_start_hour"
        private const val DEFAULT_DIARY_DAY_START_HOUR = 0
        private const val KEY_PROMPT_CUSTOMIZATIONS = "prompt_customizations_json"
        private const val KEY_PROMPT_VERSIONS = "prompt_versions_json"
        private const val KEY_API_KEY_OVERRIDE = "api_key_override"
        private const val KEY_CLOUD_SYNC_PROVIDER = "cloud_sync_provider"
        private const val KEY_CLOUD_SYNC_EMAIL = "cloud_sync_email"
        private const val KEY_LAST_SYNCED_EPOCH_MS = "last_synced_epoch_ms"
    }
}
