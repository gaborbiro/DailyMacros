package dev.gaborbiro.dailymacros.repositories.settings

import android.content.Context
import androidx.core.content.edit
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository as SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets

class SettingsRepositoryImpl(
    context: Context,
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

    companion object {
        private const val KEY_TARGETS = "targets_json"
    }
}
