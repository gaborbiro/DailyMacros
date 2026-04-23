package dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model

/**
 * Parsed from ChatGPT nutrient / recognition JSON before persistence.
 */
data class MealAnalysisComponent(
    val name: String,
    val estimatedAmount: String,
    val confidence: String,
)
