package dev.gaborbiro.dailymacros.repositories.records.domain.model.variability

data class VariabilityVariant(
    val variantKey: String,
    val variantLabel: String,
    val notesExcerpt: String,
    val evidence: List<VariabilityEvidence>,
    val sortOrder: Int,
)
