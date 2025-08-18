package dev.gaborbiro.dailymacros.features.modal.model

import android.graphics.Bitmap

data class ModalViewState(
    val imagePicker: ImagePickerState? = null,
    val closeScreen: Boolean = false,
    val dialog: DialogState? = null,
)

sealed class DialogState {
    data class EditTargetConfirmationDialog(
        val recordId: Long,
        val count: Int,
        val newTitle: String,
        val newDescription: String,
    ) : DialogState()

    data class ConfirmDestructiveChangeDialog(
        val recordId: Long,
        val newTitle: String,
        val newDescription: String,
    ) : DialogState()

    data class EditImageTargetConfirmationDialog(
        val recordId: Long,
        val count: Int,
        val image: String?,
    ) : DialogState()

    data class ViewImagesDialog(
        val title: String,
        val bitmap: Bitmap,
    ) : DialogState()

    sealed class InputDialog(
        open val validationError: String? = null,
    ) : DialogState() {

        data class CreateDialog(
            override val validationError: String? = null,
        ) : InputDialog(validationError) {
            override fun withValidationError(validationError: String?) =
                copy(validationError = validationError)
        }

        data class CreateWithImageDialog(
            val image: String?,
            val showProgressIndicator: Boolean = false,
            val suggestions: SummarySuggestions?,
            override val validationError: String? = null,
        ) : InputDialog(validationError) {
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
            val title: String,
            val description: String,
            val calories: String?,
            val protein: String?,
            val fat: String?,
            val ofWhichSaturated: String?,
            val carbs: String?,
            val ofWhichSugar: String?,
            val salt: String?,
            val fibre: String?,
            val notes: String?,
            val titleSuggestions: List<String>,
            val titleSuggestionProgressIndicator: Boolean = false,
            override val validationError: String? = null,
        ) : InputDialog(validationError) {
            override fun withValidationError(validationError: String?) =
                copy(validationError = validationError)
        }

        abstract fun withValidationError(validationError: String?): InputDialog
    }

    data class SelectRecordActionDialog(val recordId: Long) : DialogState()
    data class SelectTemplateActionDialog(val templateId: Long) : DialogState()
}

sealed class ImagePickerState {
    class ChangeImage(val recordId: Long) : ImagePickerState()
    object Select : ImagePickerState()
    object Take : ImagePickerState()
}
