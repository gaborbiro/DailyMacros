package dev.gaborbiro.dailymacros.repo.chatgpt.domain.model

data class NutrientAnalysisResponse(
    val nutrients: NutrientsApiModel?,
    val error: String?,
    val description: String?,
    val cachedTokens: Int?,
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
