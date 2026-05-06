package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.modal.model.VariabilityArchetypePickerEntry
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityArchetype

/**
 * Whether record details should show the “different meal type” variability link(s).
 * Kept as a pure function so [ModalViewModel] (and tests) own the rule; the activity only reads state.
 */
internal fun computeShowVariabilityDifferentMealLink(
    allowEdit: Boolean,
    variabilityArchetypePickerEntries: List<VariabilityArchetypePickerEntry>,
    variabilityArchetypes: List<VariabilityArchetype>,
): Boolean {
    if (!allowEdit) return false
    if (variabilityArchetypePickerEntries.isEmpty()) return false
    return variabilityArchetypes.isNotEmpty()
}
