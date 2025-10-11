package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.repo.settings.model.Targets
import kotlin.math.roundToInt

fun Targets.scaleBy(factor: Float): Targets {
    fun dev.gaborbiro.dailymacros.repo.settings.model.Target?.scaled(): dev.gaborbiro.dailymacros.repo.settings.model.Target? {
        if (this == null) return null
        val scaledMin = this.min?.let { (it * factor).roundToInt().coerceAtLeast(0) }
        val scaledMax = this.max?.let { (it * factor).roundToInt().coerceAtLeast(scaledMin ?: 0) }
        return copy(min = scaledMin, max = scaledMax)
    }

    return copy(
        calories = calories.scaled() ?: calories,
        protein = protein.scaled() ?: protein,
        fat = fat.scaled() ?: fat,
        carbs = carbs.scaled() ?: carbs,
        ofWhichSaturated = ofWhichSaturated.scaled() ?: ofWhichSaturated,
        ofWhichSugar = ofWhichSugar.scaled() ?: ofWhichSugar,
        salt = salt.scaled() ?: salt,
        fibre = fibre.scaled() ?: fibre
    )
}
