package dev.gaborbiro.dailymacros.repositories.records.domain.model.variability

data class VariabilityVariant(
    val variantKey: String,
    val variantLabel: String,
    val macroSource: String,
    val notesExcerpt: String,
    /** JSON object string for typical_macros from the model. */
    val typicalMacrosJson: String,
    val evidence: List<VariabilityEvidence>,
    val sortOrder: Int,
)
