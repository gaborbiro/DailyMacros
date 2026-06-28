package dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model

import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.common.model.TopContributors

data class NutrientAnalysisResult(
    val nutrients: Nutrients?,
    val topContributors: TopContributors?,
    val title: String?,
    val notes: String?,
    val components: List<MealComponent>,
    /** Per submitted photo index; null if model omitted `representative_of_meal` or that index. */
    val isRepresentativeOfMealByImageIndex: List<Boolean?>,
    val error: String?,
)

/**
 * Parsed from ChatGPT nutrient / recognition JSON before persistence.
 */
data class MealComponent(
    val name: String,
    val estimatedAmount: String,
    val confidence: String,
)
