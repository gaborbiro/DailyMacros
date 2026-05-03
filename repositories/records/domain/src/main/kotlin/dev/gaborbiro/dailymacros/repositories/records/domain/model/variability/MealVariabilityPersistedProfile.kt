package dev.gaborbiro.dailymacros.repositories.records.domain.model.variability

/**
 * Parsed meal-variability profile ready to replace normalized DB rows.
 */
data class MealVariabilityPersistedProfile(
    val minedAtEpochMs: Long,
    /** Full merged JSON from the model (for incremental merge and audit). */
    val profileJson: String,
    val archetypes: List<VariabilityArchetype>,
    /**
     * Max of `max(createdAtEpochMs, updatedAtEpochMs)` over templates included in the mining request
     * for this run; stored on the snapshot row for the next delta.
     */
    val templatesIngestWatermarkEpochMs: Long = 0L,
)

/**
 * Latest stored profile summary (for merging into the next mining request).
 */
data class MealVariabilityProfileSnapshot(
    val minedAtEpochMs: Long,
    val profileJson: String,
    /** Templates watermark from the last completed mine (0 if none / legacy). */
    val templatesIngestWatermarkEpochMs: Long = 0L,
)
