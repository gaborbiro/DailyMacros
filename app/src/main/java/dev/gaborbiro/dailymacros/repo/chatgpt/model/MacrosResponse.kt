package dev.gaborbiro.dailymacros.repo.chatgpt.model

data class MacrosResponse(
    val macros: MacrosApiModel?,
    val issues: String?,
    val notes: String?,
)

data class MacrosApiModel(
    val calories: Int?,
    val proteinGrams: Float?,
    val fatGrams: Float?,
    val ofWhichSaturatedGrams: Float?,
    val carbGrams: Float?,
    val ofWhichSugarGrams: Float?,
    val saltGrams: Float?,
    val fibreGrams: Float?,
)
