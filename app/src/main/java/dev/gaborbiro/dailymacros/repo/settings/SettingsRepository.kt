package dev.gaborbiro.dailymacros.repo.settings

import android.content.Context
import androidx.core.content.edit
import dev.gaborbiro.dailymacros.repo.settings.model.DEFAULT_CALORIES_MAX
import dev.gaborbiro.dailymacros.repo.settings.model.DEFAULT_CALORIES_MIN
import dev.gaborbiro.dailymacros.repo.settings.model.DEFAULT_CARBS_MAX
import dev.gaborbiro.dailymacros.repo.settings.model.DEFAULT_CARBS_MIN
import dev.gaborbiro.dailymacros.repo.settings.model.DEFAULT_FAT_MAX
import dev.gaborbiro.dailymacros.repo.settings.model.DEFAULT_FAT_MIN
import dev.gaborbiro.dailymacros.repo.settings.model.DEFAULT_FIBRE_MAX
import dev.gaborbiro.dailymacros.repo.settings.model.DEFAULT_FIBRE_MIN
import dev.gaborbiro.dailymacros.repo.settings.model.DEFAULT_PROTEIN_MAX
import dev.gaborbiro.dailymacros.repo.settings.model.DEFAULT_PROTEIN_MIN
import dev.gaborbiro.dailymacros.repo.settings.model.DEFAULT_SALT_MAX
import dev.gaborbiro.dailymacros.repo.settings.model.DEFAULT_SALT_MIN
import dev.gaborbiro.dailymacros.repo.settings.model.DEFAULT_SATURATED_MAX
import dev.gaborbiro.dailymacros.repo.settings.model.DEFAULT_SATURATED_MIN
import dev.gaborbiro.dailymacros.repo.settings.model.DEFAULT_SUGAR_MAX
import dev.gaborbiro.dailymacros.repo.settings.model.DEFAULT_SUGAR_MIN
import dev.gaborbiro.dailymacros.repo.settings.model.Target
import dev.gaborbiro.dailymacros.repo.settings.model.Targets


internal class SettingsRepository(
    context: Context,
    private val mapper: SettingsMapper,
) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun save(targets: Targets) {
        val json = mapper.map(targets)
        prefs.edit {
            putString(KEY_TARGETS, json)
        }
    }

    private val defaultTargets = Targets(
        calories = Target(true, DEFAULT_CALORIES_MIN, DEFAULT_CALORIES_MAX),
        protein = Target(true, DEFAULT_PROTEIN_MIN, DEFAULT_PROTEIN_MAX),
        salt = Target(true, DEFAULT_SALT_MIN, DEFAULT_SALT_MAX),
        fat = Target(true, DEFAULT_FAT_MIN, DEFAULT_FAT_MAX),
        carbs = Target(true, DEFAULT_CARBS_MIN, DEFAULT_CARBS_MAX),
        fibre = Target(true, DEFAULT_FIBRE_MIN, DEFAULT_FIBRE_MAX),
        ofWhichSaturated = Target(true, DEFAULT_SATURATED_MIN, DEFAULT_SATURATED_MAX),
        ofWhichSugar = Target(true, DEFAULT_SUGAR_MIN, DEFAULT_SUGAR_MAX)
    )

    fun loadTargets(): Targets {
        val json = prefs.getString(KEY_TARGETS, null) ?: return defaultTargets
        return try {
            mapper.map(json)
        } catch (e: Exception) {
            defaultTargets
        }
    }

    companion object {
        private const val KEY_TARGETS = "targets_json"
    }
}
