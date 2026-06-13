package dev.gaborbiro.dailymacros.repositories.settings

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository as SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import javax.inject.Inject
import javax.inject.Singleton

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
        val raw = runCatching { gson.fromJson<Map<String, String>>(json, type) }.getOrDefault(emptyMap())
        val migrated = raw.migrateLanguagePlaceholders()
        if (migrated != raw) setPromptCustomizations(migrated)
        return migrated
    }

    override fun setPromptCustomizations(values: Map<String, String>) {
        prefs.edit {
            putString(KEY_PROMPT_CUSTOMIZATIONS, gson.toJson(values))
        }
    }

    override fun getPromptVersions(): List<PromptVersion> {
        val json = prefs.getString(KEY_PROMPT_VERSIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<PromptVersion>>() {}.type
        val raw = runCatching { gson.fromJson<List<PromptVersion>>(json, type) }.getOrDefault(emptyList())
        val migrated = raw.map { version ->
            val migratedCustomizations = version.customizations.migrateLanguagePlaceholders()
            version.copy(customizations = migratedCustomizations)
        }
        if (migrated.zip(raw).any { (m, r) -> m.customizations != r.customizations }) {
            prefs.edit { putString(KEY_PROMPT_VERSIONS, gson.toJson(migrated)) }
        }
        return migrated
    }

    override fun deletePromptVersion(version: Int) {
        val existing = getPromptVersions()
        val updated = existing.filter { it.version != version }
        prefs.edit { putString(KEY_PROMPT_VERSIONS, gson.toJson(updated)) }
    }

    override fun savePromptVersion(customizations: Map<String, String>): PromptVersion {
        val existing = getPromptVersions()
        val nextVersion = (existing.maxOfOrNull { it.version } ?: 0) + 1
        val newVersion = PromptVersion(
            version = nextVersion,
            createdAt = System.currentTimeMillis(),
            customizations = customizations,
        )
        val updated = existing + newVersion
        prefs.edit {
            putString(KEY_PROMPT_VERSIONS, gson.toJson(updated))
            putString(KEY_PROMPT_CUSTOMIZATIONS, gson.toJson(customizations))
        }
        return newVersion
    }

    private fun Map<String, String>.migrateLanguagePlaceholders(): Map<String, String> {
        val keysToMigrate = setOf("recognition_system", "analysis_system")
        var changed = false
        val result = mapValues { (key, value) ->
            if (key in keysToMigrate && "{phone_language}" !in value) {
                val migrated = value
                    .replace("concise English title", "concise {phone_language} title")
                    .replace("MUST be in English.", "MUST be in {phone_language}.")
                    .replace("not in English,", "not in {phone_language},")
                    .replace("into English before", "into {phone_language} before")
                if (migrated != value) changed = true
                migrated
            } else {
                value
            }
        }
        return if (changed) result else this
    }

    companion object {
        private const val KEY_TARGETS = "targets_json"
        private const val KEY_DIARY_DAY_START_HOUR = "diary_day_start_hour"
        private const val DEFAULT_DIARY_DAY_START_HOUR = 0
        private const val KEY_PROMPT_CUSTOMIZATIONS = "prompt_customizations_json"
        private const val KEY_PROMPT_VERSIONS = "prompt_versions_json"
    }
}
