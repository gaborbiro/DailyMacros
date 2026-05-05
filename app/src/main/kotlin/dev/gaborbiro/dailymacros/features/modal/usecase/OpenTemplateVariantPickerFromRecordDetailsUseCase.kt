package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.repositories.records.TemplateVariabilityPreviewMapper
import dev.gaborbiro.dailymacros.repositories.records.VariabilityProfileMapper

/**
 * Builds the template variant picker dialog from the current record-details [View] state.
 */
internal class OpenTemplateVariantPickerFromRecordDetailsUseCase(
    private val variabilityProfileMapper: VariabilityProfileMapper,
    private val templateVariabilityPreviewMapper: TemplateVariabilityPreviewMapper,
) {

    fun execute(view: DialogHandle.RecordDetailsDialog.View): OpenTemplateVariantPickerResult {
        if (!view.showVariabilityDifferentMealLink) return OpenTemplateVariantPickerResult.Skipped
        val preview = view.templateVariabilityPreview ?: return OpenTemplateVariantPickerResult.Skipped
        val profileJson = view.variabilityProfileJson ?: return OpenTemplateVariantPickerResult.Skipped
        val slots = preview.slots
        if (slots.isEmpty()) return OpenTemplateVariantPickerResult.Skipped

        val archetypeKey = slots.first().archetypeKey
        val archetypeLabel = preview.archetypePickerLabel.ifBlank { slots.first().archetypeDisplayName }
        val profile = variabilityProfileMapper.parseProfileJson(
            profileJson = profileJson,
            minedAtEpochMs = view.variabilityProfileMinedAtEpochMs,
        )
        val archetype = profile.archetypes.find { it.archetypeKey == archetypeKey }
        val templateId = view.templateDbId
        val initialSelections = slots.associate { sp ->
            val slot = archetype?.slots?.find { it.slotKey == sp.slotKey }
            val variant = slot?.variants?.find { v ->
                v.evidence.any { it.templateId == templateId }
            }
            sp.slotKey to (variant?.variantKey ?: sp.variants.first().variantKey)
        }
        val existing = templateVariabilityPreviewMapper.existingCombinationKeysForArchetype(
            profile.archetypes,
            archetypeKey,
            slots,
        )
        val currentKey = templateVariabilityPreviewMapper.combinationKeyForTemplateInArchetype(
            profile.archetypes,
            archetypeKey,
            slots,
            templateId,
        )
        return OpenTemplateVariantPickerResult.Ready(
            DialogHandle.TemplateVariantPickerDialog(
                recordId = view.recordId,
                templateId = templateId,
                profileJson = profileJson,
                profileMinedAtEpochMs = view.variabilityProfileMinedAtEpochMs,
                archetypeKey = archetypeKey,
                archetypeDisplayName = archetypeLabel,
                slots = slots,
                initialSlotSelections = initialSelections,
                existingCombinationKeys = existing,
                currentTemplateCombinationKey = currentKey,
            ),
        )
    }
}

internal sealed class OpenTemplateVariantPickerResult {
    data object Skipped : OpenTemplateVariantPickerResult()
    data class Ready(val picker: DialogHandle.TemplateVariantPickerDialog) : OpenTemplateVariantPickerResult()
}
