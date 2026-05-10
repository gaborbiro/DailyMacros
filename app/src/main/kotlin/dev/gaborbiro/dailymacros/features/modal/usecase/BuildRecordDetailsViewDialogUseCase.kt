package dev.gaborbiro.dailymacros.features.modal.usecase

import androidx.compose.ui.text.input.TextFieldValue
import dev.gaborbiro.dailymacros.features.modal.ModalUiMapper
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.VariabilityArchetypePickerEntry
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityPreviewContent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityArchetype

/**
 * Builds the read-only record details dialog for a loaded [Record], including variability preview
 * when [edit] is true (swallows [NoVariabilityProfileLoadedException] the same way as before).
 */
internal class BuildRecordDetailsViewDialogUseCase(
    private val getVariabilityMatchForTemplateUseCase: GetVariabilityMatchForTemplateUseCase,
    private val uiMapper: ModalUiMapper,
) {

    suspend fun execute(record: Record, edit: Boolean): DialogHandle.RecordDetailsDialog.View {
        val (previewForDialog, variabilityArchetypes, archetypePickerEntries) = if (edit) {
            val match = runCatching {
                getVariabilityMatchForTemplateUseCase.execute(record.template.dbId)
            }.getOrElse { t ->
                if (t is NoVariabilityProfileLoadedException) {
                    TemplateVariabilityMatch(
                        preview = TemplateVariabilityPreviewContent(
                            bannerText = "",
                            slots = emptyList(),
                            archetypePickerLabel = "",
                        ),
                        variabilityArchetypes = emptyList(),
                        archetypePickerEntries = emptyList(),
                    )
                } else {
                    throw t
                }
            }
            Triple(
                match.preview.slots.takeIf { it.isNotEmpty() }?.let { match.preview },
                match.variabilityArchetypes,
                match.archetypePickerEntries,
            )
        } else {
            Triple(null, emptyList(), emptyList())
        }

        return DialogHandle.RecordDetailsDialog.View(
            recordId = record.recordId,
            templateDbId = record.template.dbId,
            title = TextFieldValue(record.template.name),
            description = TextFieldValue(record.template.description),
            images = record.template.images,
            nutrientBreakdown = uiMapper.mapNutrientBreakdowns(record),
            allowEdit = edit,
            titleHint = "Give your meal a title",
            titleValidationError = null,
            templateVariabilityPreview = previewForDialog,
            variabilityArchetypes = variabilityArchetypes,
            variabilityArchetypePickerEntries = archetypePickerEntries,
            showVariabilityDifferentMealLink = uiMapper.mapShowVariabilityDifferentMealLink(
                allowEdit = edit,
                variabilityArchetypePickerEntries = archetypePickerEntries,
                variabilityArchetypes = variabilityArchetypes,
            ),
        )
    }
}
