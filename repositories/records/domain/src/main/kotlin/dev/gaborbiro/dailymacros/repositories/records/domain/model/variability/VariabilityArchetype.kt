package dev.gaborbiro.dailymacros.repositories.records.domain.model.variability

data class VariabilityArchetype(
    val archetypeKey: String,
    val displayName: String,
    val titleAliasesJson: String,
    val evidenceCount: Int,
    val lastSeenTimestamp: String?,
    val archetypeNotes: String?,
    val deprecated: Boolean,
    val deprecatedReason: String?,
    val slots: List<VariabilitySlot>,
    val sortOrder: Int,
)
