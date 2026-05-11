package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.repositories.records.TemplateVariabilityPreviewMapper

/**
 * Builds the template variant picker dialog from the current record-details [View] state.
 */
class OpenTemplateVariantPickerFromRecordDetailsUseCase(
    private val templateVariabilityPreviewMapper: TemplateVariabilityPreviewMapper,
) {

    fun execute(
        view: DialogHandle.RecordDetailsDialog.View,
        archetypeKey: String,
    ): OpenTemplateVariantPickerResult {
        if (!view.showVariabilityDifferentMealLink) return OpenTemplateVariantPickerResult.Skipped
        if (view.variabilityArchetypes.isEmpty()) return OpenTemplateVariantPickerResult.Skipped
        val entry = view.variabilityArchetypePickerEntries.find { it.archetypeKey == archetypeKey }
            ?: return OpenTemplateVariantPickerResult.Skipped
        val slots = entry.slots
        if (slots.isEmpty()) return OpenTemplateVariantPickerResult.Skipped

        val archetypes = view.variabilityArchetypes
        val archetype = archetypes.find { it.archetypeKey == archetypeKey }
        val templateId = view.templateDbId
        val initialSelections = slots.associate { sp ->
            val slot = archetype?.slots?.find { it.slotKey == sp.slotKey }
            val variant = slot?.variants?.find { v ->
                v.evidence.any { it.templateId == templateId }
            }
            sp.slotKey to (variant?.variantKey ?: sp.variants.first().variantKey)
        }
        val existing = templateVariabilityPreviewMapper.existingCombinationKeysForArchetype(
            archetypes,
            archetypeKey,
            slots,
        )
        val currentKey = templateVariabilityPreviewMapper.combinationKeyForTemplateInArchetype(
            archetypes,
            archetypeKey,
            slots,
            templateId,
        )
        return OpenTemplateVariantPickerResult.Ready(
            DialogHandle.TemplateVariantPickerDialog(
                recordId = view.recordId,
                templateId = templateId,
                variabilityArchetypes = archetypes,
                archetypeKey = archetypeKey,
                archetypeDisplayName = entry.linkTitle,
                slots = slots,
                initialSlotSelections = initialSelections,
                existingCombinationKeys = existing,
                currentTemplateCombinationKey = currentKey,
            ),
        )
    }
}

sealed class OpenTemplateVariantPickerResult {
    data object Skipped : OpenTemplateVariantPickerResult()
    data class Ready(val picker: DialogHandle.TemplateVariantPickerDialog) : OpenTemplateVariantPickerResult()
}
