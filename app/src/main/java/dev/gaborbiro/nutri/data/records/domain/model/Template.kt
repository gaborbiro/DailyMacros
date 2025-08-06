package dev.gaborbiro.nutri.data.records.domain.model

data class Template(
    val id: Long,
    val image: String?,
    val name: String,
    val description: String,
    val nutrients: Nutrients?,
)

data class Nutrients(
    val calories: Int?,
    val carbohydrates: Float?,
    val fat: Float?,
    val protein: Float?,
)
