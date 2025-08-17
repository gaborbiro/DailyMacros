package dev.gaborbiro.dailymacros.repo.chatgpt.model

data class NutrientsResponse(
    val nutrients: NutrientApiModel?,
    val issues: String?,
    val notes: String?,
)

data class NutrientApiModel(
    val calories: Int?,
    val proteinGrams: Float?,
    val carbGrams: Float?,
    val ofWhichSugarGrams: Float?,
    val fatGrams: Float?,
    val ofWhichSaturatedGrams: Float?,
    val saltGrams: Float?,
    val fibreGrams: Float?,
)
