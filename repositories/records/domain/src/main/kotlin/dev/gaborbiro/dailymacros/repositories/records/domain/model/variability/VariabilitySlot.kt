package dev.gaborbiro.dailymacros.repositories.records.domain.model.variability

data class VariabilitySlot(
    val slotKey: String,
    /** Human-readable slot caption for UI (from model `role_display_name`). */
    val roleDisplayName: String,
    val nutritionalLeversJson: String,
    val isHighVariability: Boolean,
    val confidence: Double,
    val rationale: String,
    val variants: List<VariabilityVariant>,
    val sortOrder: Int,
)
