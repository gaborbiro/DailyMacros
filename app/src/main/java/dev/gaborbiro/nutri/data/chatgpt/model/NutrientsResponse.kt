package dev.gaborbiro.nutri.data.chatgpt.model

data class NutrientsResponse(
    val nutrients: NutrientApiModel?,
    val comments: String,
)

data class NutrientApiModel(
    val kcal: Int?,
    val proteinGrams: Float?,
    val carbGrams: Float?,
    val fatGrams: Float?,
)
