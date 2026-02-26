package dev.gaborbiro.dailymacros.repo.records.domain.model

/**
 * null doesn't mean 0 for that nutrient. It means it's unknown.
 */
data class NutrientBreakdown(
    val calories: Int? = null,
    val protein: Float? = null,
    val fat: Float? = null,
    val ofWhichSaturated: Float? = null,
    val carbs: Float? = null,
    val ofWhichSugar: Float? = null,
    val ofWhichAddedSugar: Float? = null,
    val salt: Float? = null,
    val fibre: Float? = null,
)
