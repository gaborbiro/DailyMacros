package dev.gaborbiro.dailymacros.repositories.records.domain.model

/**
 * One line item from AI nutrient analysis (or recognition), stored structurally.
 * [estimatedAmount] is the free-text amount from the model (e.g. "125 g", "1 tbsp").
 */
data class MealComponent(
    val name: String,
    val estimatedAmount: String,
    val confidence: ComponentConfidence,
)

enum class ComponentConfidence {
    HIGH,
    MEDIUM,
    LOW,
}
