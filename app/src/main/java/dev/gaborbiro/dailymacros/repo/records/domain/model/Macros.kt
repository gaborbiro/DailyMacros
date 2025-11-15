package dev.gaborbiro.dailymacros.repo.records.domain.model

/**
 * null doesn't mean 0 for that macro. It means it's unknown.
 */
data class Macros(
    val calories: Int?,
    val protein: Float?,
    val fat: Float?,
    val ofWhichSaturated: Float?,
    val carbs: Float?,
    val ofWhichSugar: Float?,
    val salt: Float?,
    val fibre: Float?,
    val notes: String?,
)
