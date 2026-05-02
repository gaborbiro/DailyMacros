package dev.gaborbiro.dailymacros.repositories.records.domain.model

/**
 * One variant option for the template variability preview UI (mined profile).
 */
data class TemplateVariabilityVariantPreview(
    val variantKey: String,
    val variantLabel: String,
)

/**
 * One slot row with variants that include [templateId] in evidence (for quick-pick preview).
 */
data class TemplateVariabilitySlotPreview(
    val archetypeKey: String,
    val archetypeDisplayName: String,
    val slotKey: String,
    val role: String,
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
