package dev.gaborbiro.dailymacros.repo.settings

import android.content.Context
import androidx.core.content.edit
import dev.gaborbiro.dailymacros.repo.settings.model.THEORETICAL_CALORIES_MAX
import dev.gaborbiro.dailymacros.repo.settings.model.THEORETICAL_CARBS_MAX
import dev.gaborbiro.dailymacros.repo.settings.model.THEORETICAL_FAT_MAX
import dev.gaborbiro.dailymacros.repo.settings.model.THEORETICAL_FIBRE_MAX
import dev.gaborbiro.dailymacros.repo.settings.model.THEORETICAL_PROTEIN_MAX
import dev.gaborbiro.dailymacros.repo.settings.model.THEORETICAL_SALT_MAX
import dev.gaborbiro.dailymacros.repo.settings.model.THEORETICAL_SATURATED_MAX
import dev.gaborbiro.dailymacros.repo.settings.model.THEORETICAL_SUGAR_MAX
import dev.gaborbiro.dailymacros.repo.settings.model.Target
import dev.gaborbiro.dailymacros.repo.settings.model.Targets


internal class SettingsRepository(
    context: Context,
    private val mapper: SettingsMapper,
) {
    private val prefs = context.getSharedPreferences("settings2", Context.MODE_PRIVATE)

    fun save(targets: Targets) {
        val json = mapper.map(targets)
        prefs.edit {
            putString(KEY_TARGETS, json)
        }
    }

    private val defaultTargets = Targets(
        calories = Target(),
        protein = Target(true),
        salt = Target(true),
        fibre = Target(true),
        fat = Target(true),
        ofWhichSaturated = Target(true),
        carbs = Target(true),
        ofWhichSugar = Target(true)
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
