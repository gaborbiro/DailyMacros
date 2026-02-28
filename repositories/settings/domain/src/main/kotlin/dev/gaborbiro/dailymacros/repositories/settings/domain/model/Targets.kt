package dev.gaborbiro.dailymacros.repositories.settings.domain.model

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
    val enabled: Boolean,
    val min: Int? = null,
    val max: Int? = null,
)
