package dev.gaborbiro.dailymacros.repositories.settings

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository as SettingsRepository
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
        return runCatching { Gson().fromJson<Map<String, String>>(json, type) }.getOrDefault(emptyMap())
    }

    override fun setPromptCustomizations(values: Map<String, String>) {
        prefs.edit {
            putString(KEY_PROMPT_CUSTOMIZATIONS, Gson().toJson(values))
        }
    }

    companion object {
        private const val KEY_TARGETS = "targets_json"
        private const val KEY_DIARY_DAY_START_HOUR = "diary_day_start_hour"
        private const val DEFAULT_DIARY_DAY_START_HOUR = 0
        private const val KEY_PROMPT_CUSTOMIZATIONS = "prompt_customizations_json"
    }
}
