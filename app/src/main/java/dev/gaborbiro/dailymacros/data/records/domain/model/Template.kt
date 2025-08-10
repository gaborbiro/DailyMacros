package dev.gaborbiro.dailymacros.data.records.domain.model

data class Template(
    val id: Long,
    val image: String?,
    val name: String,
    val description: String,
    val nutrients: Nutrients?,
)

data class Nutrients(
    val calories: Int?,
    val protein: Float?,
    val carbohydrates: Float?,
    val ofWhichSugar: Float?,
    val fat: Float?,
    val ofWhichSaturated: Float?,
    val salt: Float?,
)
