package dev.gaborbiro.dailymacros.repositories.records.domain.model.variability

data class VariabilityEvidence(
    val loggedAt: String,
    val templateId: Long?,
    /** Diary template title from the supporting meal_observations row (optional for legacy profiles). */
    val title: String? = null,
)
