package dev.gaborbiro.dailymacros.features.modal.model

import androidx.compose.ui.text.input.TextFieldValue
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel

data class ModalUiState(
    val rootDialog: DialogHandle? = null,
    val overlayDialog: DialogHandle? = null,
    val photoExportInProgress: Boolean = false,
)

sealed class DialogHandle {
    data class EditTargetConfirmationDialog(
        val recordId: Long,
        val count: Int,
        val images: List<String>,
        val title: String,
        val description: String,
    ) : DialogHandle()

    data class ViewImageDialog(
        val title: String,
        val images: List<String>,
        val initialPage: Int = 0,
    ) : DialogHandle()

    sealed class RecordDetailsDialog(
        open val titleHint: String,
        open val titleValidationError: String? = null,
        open val title: TextFieldValue,
        open val description: TextFieldValue,
        open val images: List<String>,
    ) : DialogHandle() {

        data class Edit(
            override val title: TextFieldValue,
            override val titleHint: String,
            override val titleValidationError: String? = null,
            override val description: TextFieldValue,
            override val images: List<String>,
            val showProgressIndicator: Boolean = false,
            val showRunAIButton: Boolean = false,
            val recognisedFood: RecognisedFood?,
            val pristineSnapshot: RecordDetailsPristineSnapshot,
        ) : RecordDetailsDialog(
            titleHint = titleHint,
            titleValidationError = titleValidationError,
            title = title,
            description = description,
            images = images,
        ) {
            override fun withTitleValidationError(titleValidationError: String?) =
                copy(titleValidationError = titleValidationError)

            override fun withTitle(title: TextFieldValue) = copy(title = title)
            override fun withDescription(description: TextFieldValue) =
                copy(description = description)
        }

        data class View(
            val recordId: Long,
            val templateDbId: Long,
            /** Source template for fork when opened from quick-pick template details (Add). */
            val variabilityAnchorTemplateDbId: Long,
            val nutrientBreakdown: NutrientBreakdownUiModel?,
            val compactNutrients: NutrientsUiModel,
            val allowEdit: Boolean,
            val isEditing: Boolean = false,
            override val titleHint: String,
            override val titleValidationError: String? = null,
            override val title: TextFieldValue,
            override val description: TextFieldValue,
            override val images: List<String>,
            /** Opened from quick-pick template “Details”; submit creates a new template + new record. */
            val openedFromTemplateDetailsOnly: Boolean = false,
            /** When non-null, template has other logged variants; combo is shown above the title field. */
            val variantPickerOptions: List<MealVariantPickerOption>? = null,
            /** Whether this template is shown as a Quick Pick (override or natural ranking). */
            val quickPickStarred: Boolean = false,
            val linkedRecordCountForTemplate: Int = 0,
            val pristineSnapshot: RecordDetailsPristineSnapshot,
        ) : RecordDetailsDialog(
            titleHint = titleHint,
            titleValidationError = titleValidationError,
            title = title,
            description = description,
            images = images,
        ) {
            override fun withTitleValidationError(titleValidationError: String?) =
                copy(titleValidationError = titleValidationError)

            override fun withTitle(title: TextFieldValue) = copy(title = title)
            override fun withDescription(description: TextFieldValue) =
                copy(description = description)
        }

        abstract fun withTitleValidationError(titleValidationError: String?): RecordDetailsDialog
        abstract fun withTitle(title: TextFieldValue): RecordDetailsDialog
        abstract fun withDescription(description: TextFieldValue): RecordDetailsDialog
    }

    data class ImageInput(val type: ImageInputType) : DialogHandle()

    data class ConfirmSwitchTemplateDialog(
        val pendingTemplateId: Long,
    ) : DialogHandle()

    data class InfoDialog(val message: String) : DialogHandle()
}

/** Baseline for unsaved detection on create-record [DialogHandle.RecordDetailsDialog.Edit]. */
fun recordDetailsEditPristineSnapshot(
    title: TextFieldValue,
    description: TextFieldValue,
    images: List<String>,
): RecordDetailsPristineSnapshot =
    RecordDetailsPristineSnapshot(
        templateDbId = 0L,
        title = title.text,
        description = description.text,
        images = images,
    )

/** True when title, description, or images differ from when the dialog was opened. */
fun DialogHandle.RecordDetailsDialog.hasUnsavedEdits(): Boolean {
    val p = when (this) {
        is DialogHandle.RecordDetailsDialog.View -> pristineSnapshot
        is DialogHandle.RecordDetailsDialog.Edit -> pristineSnapshot
    }
    return title.text != p.title ||
        description.text != p.description ||
        images != p.images
}

/** True when photos were added or removed (not merely reordered). */
fun imagesRequireMacroReanalysis(pristine: List<String>, current: List<String>): Boolean =
    pristine.toSet() != current.toSet()

data class RecognisedFood(
    val title: String?,
)

sealed class ImageInputType {
    data object Camera : ImageInputType()
    data object BrowseImages : ImageInputType()
}

data class NutrientBreakdownUiModel(
    val calories: String?,
    val protein: String?,
    val fat: String?,
    val ofWhichSaturated: String?,
    val carbs: String?,
    val ofWhichSugar: String?,
    val ofWhichAddedSugar: String?,
    val salt: String?,
    val fibre: String?,
    val notes: String?,
    val components: List<String> = emptyList(),
)

/** True when the breakdown has at least one macro line, non-blank AI notes, or components to show. */
fun NutrientBreakdownUiModel.hasDisplayableContent(): Boolean =
    sequenceOf(
        calories,
        protein,
        fat,
        ofWhichSaturated,
        carbs,
        ofWhichSugar,
        ofWhichAddedSugar,
        salt,
        fibre,
    ).any { !it.isNullOrBlank() } || !notes.isNullOrBlank() || components.isNotEmpty()
