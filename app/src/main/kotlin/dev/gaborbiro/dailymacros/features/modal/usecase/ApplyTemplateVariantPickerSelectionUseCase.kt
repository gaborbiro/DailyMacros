package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.repositories.records.TemplateVariabilityPreviewMapper
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository

/**
 * Resolves a slot combination from the variant picker to an existing template and updates the record.
 */
internal class ApplyTemplateVariantPickerSelectionUseCase(
    private val recordsRepository: RecordsRepository,
    private val templateVariabilityPreviewMapper: TemplateVariabilityPreviewMapper,
) {

    suspend fun execute(
        picker: DialogHandle.TemplateVariantPickerDialog,
        slotKeyToVariantKey: Map<String, String>,
    ): ApplyTemplateVariantPickerSelectionResult {
        val comboKey = templateVariabilityPreviewMapper.combinationKey(picker.slots, slotKeyToVariantKey)
        val reuseId = templateVariabilityPreviewMapper.templateIdForCombinationInArchetype(
            picker.variabilityArchetypes,
            picker.archetypeKey,
            picker.slots,
            comboKey,
        ) ?: return ApplyTemplateVariantPickerSelectionResult.UnknownCombination

        val record = recordsRepository.get(picker.recordId)
            ?: return ApplyTemplateVariantPickerSelectionResult.RecordNotFound

        if (reuseId == record.template.dbId) {
            return ApplyTemplateVariantPickerSelectionResult.NoOpSameTemplate
        }

        val newTemplate = recordsRepository.getTemplate(reuseId)
        recordsRepository.updateRecord(record.copy(template = newTemplate))
        return ApplyTemplateVariantPickerSelectionResult.Applied
    }
}

internal sealed class ApplyTemplateVariantPickerSelectionResult {
    data object UnknownCombination : ApplyTemplateVariantPickerSelectionResult()
    data object RecordNotFound : ApplyTemplateVariantPickerSelectionResult()
    /** Selected combination maps to the record’s current template. */
    data object NoOpSameTemplate : ApplyTemplateVariantPickerSelectionResult()
    /** Record now points at the resolved template. */
    data object Applied : ApplyTemplateVariantPickerSelectionResult()
}
