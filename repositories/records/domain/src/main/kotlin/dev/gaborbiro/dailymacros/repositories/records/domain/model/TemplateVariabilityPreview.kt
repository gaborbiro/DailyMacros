package dev.gaborbiro.dailymacros.repositories.records.domain.model

/**
 * One variant option for the template variability preview UI (mined profile).
 */
data class TemplateVariabilityVariantPreview(
    val variantKey: String,
    val variantLabel: String,
)

/**
 * One slot row for quick-pick preview: the slot is included because at least one variant cites the
 * template in evidence. [variants] lists every variant in the slot, ordered with variants that cite
 * the starting template first (then the rest by profile sort order) so the UI can default the
 * dropdown to the current meal’s choice.
 */
data class TemplateVariabilitySlotPreview(
    val archetypeKey: String,
    val archetypeDisplayName: String,
    val slotKey: String,
    val roleDisplayName: String,
    val variants: List<TemplateVariabilityVariantPreview>,
)

/**
 * Payload for the variability preview dialog before adding from a template.
 */
data class TemplateVariabilityPreviewContent(
    /** Shown when [slots] is empty (no profile, or no matching slots). */
    val bannerText: String,
    val slots: List<TemplateVariabilitySlotPreview>,
)
