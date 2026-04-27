package dev.gaborbiro.dailymacros.repositories.records.domain.model.variability

data class VariabilitySlot(
    val slotKey: String,
    val role: String,
    val nutritionalLeversJson: String,
    val isHighVariability: Boolean,
    val confidence: Double,
    val rationale: String,
    val variants: List<VariabilityVariant>,
    val sortOrder: Int,
)
