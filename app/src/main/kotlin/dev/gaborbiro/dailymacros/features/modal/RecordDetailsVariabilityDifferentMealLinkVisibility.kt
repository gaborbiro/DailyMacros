package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityPreviewContent

/**
 * Whether record details should show the “different meal type” variability link.
 * Kept as a pure function so [ModalViewModel] (and tests) own the rule; the activity only reads state.
 */
internal fun computeShowVariabilityDifferentMealLink(
    allowEdit: Boolean,
    templateVariabilityPreview: TemplateVariabilityPreviewContent?,
    variabilityProfileJson: String?,
): Boolean {
    if (!allowEdit) return false
    val preview = templateVariabilityPreview ?: return false
    if (preview.slots.isEmpty()) return false
    if (preview.archetypePickerLabel.isBlank()) return false
    return !variabilityProfileJson.isNullOrBlank()
}
