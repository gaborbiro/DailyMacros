package dev.gaborbiro.dailymacros.features.modal.usecase

import androidx.compose.ui.text.input.TextFieldValue
import dev.gaborbiro.dailymacros.features.modal.ModalUiMapper
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.RecordDetailsPristineSnapshot
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import javax.inject.Inject

/**
 * Builds the record details dialog for a loaded [Record].
 */
class BuildRecordDetailsViewDialogUseCase @Inject constructor(
    private val uiMapper: ModalUiMapper,
) {

    fun execute(
        record: Record,
        edit: Boolean,
        templateDetailsMode: Boolean = false,
    ): DialogHandle.RecordDetailsDialog.View {
        val allowEdit = edit || templateDetailsMode
        val tmpl = record.template
        return DialogHandle.RecordDetailsDialog.View(
            recordId = record.recordId,
            templateDbId = tmpl.dbId,
            variabilityAnchorTemplateDbId = tmpl.dbId,
            title = TextFieldValue(tmpl.name),
            description = TextFieldValue(tmpl.description),
            imageFilenames = tmpl.imageFilenames,
            nutrientBreakdown = uiMapper.mapNutrientBreakdowns(record),
            compactNutrients = uiMapper.mapCompactNutrients(record),
            showLoadingIndicator = tmpl.isPending,
            allowEdit = allowEdit,
            titleHint = "Title",
            titleValidationError = null,
            openedFromTemplateDetailsOnly = templateDetailsMode,
            variantPickerOptions = null,
            quickPickStarred = false,
            linkedRecordCountForTemplate = 0,
            pristineSnapshot = RecordDetailsPristineSnapshot(
                templateDbId = tmpl.dbId,
                title = tmpl.name,
                description = tmpl.description,
                imageFilenames = tmpl.imageFilenames,
            ),
            isEditing = false,
        )
    }
}
