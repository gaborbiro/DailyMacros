package dev.gaborbiro.dailymacros.features.settings.targetsSettings

import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.FieldErrors
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.MacroType
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.TargetUiModel
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.TargetsSettingsUiState
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.THEORETICAL_CALORIES_MAX
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.THEORETICAL_CARBS_MAX
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.THEORETICAL_FAT_MAX
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.THEORETICAL_FIBRE_MAX
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.THEORETICAL_PROTEIN_MAX
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.THEORETICAL_SALT_MAX
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.THEORETICAL_SATURATED_MAX
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.THEORETICAL_SUGAR_MAX
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets

class TargetsSettingsUIMapper {

    fun map(
        targets: Targets,
        canReset: Boolean = false,
        canSave: Boolean = false,
        showExitDialog: Boolean = false,
        errors: Map<MacroType, FieldErrors> = emptyMap(),
    ): TargetsSettingsUiState {
        return TargetsSettingsUiState(
            targets = buildMap {
                put(MacroType.CALORIES, map(MacroType.CALORIES, targets.calories))
                put(MacroType.PROTEIN, map(MacroType.PROTEIN, targets.protein))
                put(MacroType.SALT, map(MacroType.SALT, targets.salt))
                put(MacroType.FIBRE, map(MacroType.FIBRE, targets.fibre))
                put(MacroType.FAT, map(MacroType.FAT, targets.fat))
                put(MacroType.SATURATED, map(MacroType.SATURATED, targets.ofWhichSaturated))
                put(MacroType.CARBS, map(MacroType.CARBS, targets.carbs))
                put(MacroType.SUGAR, map(MacroType.SUGAR, targets.ofWhichSugar))
            },
            canReset = canReset,
            canSave = canSave,
            showExitDialog = showExitDialog,
            errors = errors,
        )
    }

    private fun map(type: MacroType, target: Target): TargetUiModel {
        return TargetUiModel(
            enabled = target.enabled,
            min = target.min,
            max = target.max,
            theoreticalMax = mapTheoreticalMax(type),
        )
    }

    private fun mapTheoreticalMax(type: MacroType): Int {
        return when (type) {
            MacroType.CALORIES -> THEORETICAL_CALORIES_MAX
            MacroType.PROTEIN -> THEORETICAL_PROTEIN_MAX
            MacroType.SALT -> THEORETICAL_SALT_MAX
            MacroType.FAT -> THEORETICAL_FAT_MAX
            MacroType.CARBS -> THEORETICAL_CARBS_MAX
            MacroType.FIBRE -> THEORETICAL_FIBRE_MAX
            MacroType.SATURATED -> THEORETICAL_SATURATED_MAX
            MacroType.SUGAR -> THEORETICAL_SUGAR_MAX
        }
    }

    fun map(targetsSettingsUiState: TargetsSettingsUiState): Targets {
        val targets: Map<MacroType, Target> = targetsSettingsUiState.targets.mapValues { (_, target) ->
            map(target)
        }
        return Targets(
            calories = targets[MacroType.CALORIES]!!,
            protein = targets[MacroType.PROTEIN]!!,
            salt = targets[MacroType.SALT]!!,
            fibre = targets[MacroType.FIBRE]!!,
            fat = targets[MacroType.FAT]!!,
            ofWhichSaturated = targets[MacroType.SATURATED]!!,
            carbs = targets[MacroType.CARBS]!!,
            ofWhichSugar = targets[MacroType.SUGAR]!!,
        )
    }

    private fun map(uiTarget: TargetUiModel): Target {
        return Target(
            enabled = uiTarget.enabled,
            min = uiTarget.min,
            max = uiTarget.max,
        )
    }
}
