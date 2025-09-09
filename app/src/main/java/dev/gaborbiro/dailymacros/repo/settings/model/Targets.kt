package dev.gaborbiro.dailymacros.repo.settings.model

data class Targets(
    val calories: Target,
    val protein: Target,
    val salt: Target,
    val fat: Target,
    val carbs: Target,
    val fibre: Target,
    val ofWhichSaturated: Target,
    val ofWhichSugar: Target,
)

data class Target(
    val enabled: Boolean = true,
    val min: Int,
    val max: Int,
)
