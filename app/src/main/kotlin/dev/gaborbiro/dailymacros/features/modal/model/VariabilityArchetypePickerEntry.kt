package dev.gaborbiro.dailymacros.features.modal.model

import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilitySlotPreview

/**
 * One variability archetype that cites the current template, with only that archetype’s slot rows.
 * Used for a separate “different meal type” link and variant picker per archetype.
 */
data class VariabilityArchetypePickerEntry(
    val archetypeKey: String,
    /** Shown in the link, e.g. “Looking for a different {linkTitle}?” */
    val linkTitle: String,
    val slots: List<TemplateVariabilitySlotPreview>,
)
