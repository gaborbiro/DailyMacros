package dev.gaborbiro.dailymacros.features.modal.model

import android.graphics.Bitmap

data class ModalViewState(
    val dialogs: List<DialogState> = emptyList(),
    val close: Boolean = false,
)

sealed class DialogState {
    data class EditTargetConfirmationDialog(
        val recordId: Long,
        val count: Int,
        val images: List<String>,
        val title: String,
        val description: String,
    ) : DialogState()

    data class ViewImageDialog(
        val title: String,
        val bitmap: Bitmap,
    ) : DialogState()

    sealed class InputDialog(
        open val titleHint: String,
        open val validationError: String? = null,
    ) : DialogState() {

        data class CreateDialog(
            override val titleHint: String,
            override val validationError: String? = null,
        ) : InputDialog(
            titleHint = titleHint,
            validationError = validationError,
        ) {
            override fun withValidationError(validationError: String?) =
                copy(validationError = validationError)
        }

        data class CreateWithImageDialog(
            val images: List<String>,
            val showProgressIndicator: Boolean = false,
            val suggestions: SummarySuggestions?,
            override val titleHint: String,
            override val validationError: String? = null,
        ) : InputDialog(
            titleHint = titleHint,
            validationError = validationError,
        ) {
            override fun withValidationError(validationError: String?) =
                copy(validationError = validationError)
        }

        data class SummarySuggestions(
            val titles: List<String>,
            val description: String?,
        )

        data class RecordDetailsDialog(
            val recordId: Long,
            val images: List<String>,
            val title: String?,
            val description: String?,
            val macros: MacrosUIModel?,
            val titleSuggestions: List<String>,
            val allowEdit: Boolean,
            override val titleHint: String,
            override val validationError: String? = null,
        ) : InputDialog(
            titleHint = titleHint,
            validationError = validationError,
        ) {
            override fun withValidationError(validationError: String?) =
                copy(validationError = validationError)
        }

        abstract fun withValidationError(validationError: String?): InputDialog
    }

    data class NewImage(val imagePickerState: ImagePickerState) : DialogState()

    data class SelectRecordActionDialog(val recordId: Long) : DialogState()

    data class SelectTemplateActionDialog(val templateId: Long) : DialogState()

    data class ErrorDialog(val errorMessage: String) : DialogState()

    data class InfoDialog(val message: String) : DialogState()
}

sealed class ImagePickerState(open val recordId: Long?) {
    data class Select(override val recordId: Long?) : ImagePickerState(recordId)
    data class Take(override val recordId: Long?) : ImagePickerState(recordId)
}

data class MacrosUIModel(
    val calories: String?,
    val protein: String?,
    val fat: String?,
    val ofWhichSaturated: String?,
    val carbs: String?,
    val ofWhichSugar: String?,
    val salt: String?,
    val fibre: String?,
    val notes: String?,
)
