package dev.gaborbiro.dailymacros.repo.records.domain.model

/**
 * null doesn't mean 0 for that nutrient. It means it's unknown.
 */
data class Nutrients(
    val calories: Int?,
    val protein: Float?,
    val carbohydrates: Float?,
    val ofWhichSugar: Float?,
    val fat: Float?,
    val ofWhichSaturated: Float?,
    val salt: Float?,
    val fibre: Float?,
)
