package dev.gaborbiro.dailymacros.features.settings

import dev.gaborbiro.dailymacros.features.settings.model.MacroType
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUIModel
import dev.gaborbiro.dailymacros.features.settings.model.TargetUIModel
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

internal class UIMapper {

    fun map(targets: Targets): SettingsUIModel {
        return SettingsUIModel(
            buildMap {
                put(MacroType.CALORIES, map(MacroType.CALORIES, targets.calories))
                put(MacroType.PROTEIN, map(MacroType.PROTEIN, targets.protein))
                put(MacroType.SALT, map(MacroType.SALT, targets.salt))
                put(MacroType.FIBRE, map(MacroType.FIBRE, targets.fibre))
                put(MacroType.FAT, map(MacroType.FAT, targets.fat))
                put(MacroType.SATURATED, map(MacroType.SATURATED, targets.ofWhichSaturated))
                put(MacroType.CARBS, map(MacroType.CARBS, targets.carbs))
                put(MacroType.SUGAR, map(MacroType.SUGAR, targets.ofWhichSugar))
            }
        )
    }

    private fun map(type: MacroType, target: Target): TargetUIModel {
        return TargetUIModel(
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

    fun map(settings: SettingsUIModel): Targets {
        val targets: Map<MacroType, Target> = settings.targets.mapValues { (_, target) ->
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

    private fun map(uiTarget: TargetUIModel): Target {
        return Target(
            enabled = uiTarget.enabled,
            min = uiTarget.min!!,
            max = uiTarget.max!!,
        )
    }
}
