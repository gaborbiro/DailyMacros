package dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model

data class NutrientAnalysisResult(
    val nutrients: NutrientsApiModel?,
    val title: String?,
    val notes: String?,
    val cachedTokens: Int?,
    val error: String?,
)

data class NutrientsApiModel(
    val calories: Int?,
    val protein: NutrientApiModel?,
    val fat: NutrientApiModel?,
    val ofWhichSaturated: NutrientApiModel?,
    val carb: NutrientApiModel?,
    val ofWhichSugar: NutrientApiModel?,
    val ofWhichAddedSugar: NutrientApiModel?,
    val salt: NutrientApiModel?,
    val fibre: NutrientApiModel?,
)

data class NutrientApiModel(
    val grams: Float?,
    val topContributors: String?,
)
