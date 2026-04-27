package dev.gaborbiro.dailymacros.repositories.records.domain.model.variability

/**
 * Parsed meal-variability profile ready to replace normalized DB rows.
 */
data class MealVariabilityPersistedProfile(
    val minedAtEpochMs: Long,
    /** Full merged JSON from the model (for incremental merge and audit). */
    val profileJson: String,
    val archetypes: List<VariabilityArchetype>,
)

/**
 * Latest stored profile summary (for merging into the next mining request).
 */
data class MealVariabilityProfileSnapshot(
    val minedAtEpochMs: Long,
    val profileJson: String,
)
