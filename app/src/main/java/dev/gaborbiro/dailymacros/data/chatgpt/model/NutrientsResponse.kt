package dev.gaborbiro.dailymacros.data.chatgpt.model

data class NutrientsResponse(
    val nutrients: NutrientApiModel?,
    val comments: String,
)

data class NutrientApiModel(
    val calories: Int?,
    val proteinGrams: Float?,
    val carbGrams: Float?,
    val ofWhichSugarGrams: Float?,
    val fatGrams: Float?,
    val ofWhichSaturatedGrams: Float?,
    val saltGrams: Float?,
)
