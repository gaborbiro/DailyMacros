package dev.gaborbiro.dailymacros.features.modal.model

import android.graphics.Bitmap

data class HostViewState(
    val showCamera: Boolean = false,
    val imagePicker: ImagePickerState? = null,
    val refreshWidget: Boolean = false,
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

    data class EditImageTargetConfirmationDialog(
        val recordId: Long,
        val count: Int,
        val image: String?,
    ) : DialogState()

    data class ViewImageDialog(val bitmap: Bitmap) : DialogState()

    sealed class InputDialog(
        open val validationError: String? = null,
    ) : DialogState() {

        data class Create(
            override val validationError: String? = null,
        ) : InputDialog(validationError) {
            override fun withValidationError(validationError: String?) =
                copy(validationError = validationError)
        }

        data class CreateWithImage(
            val image: String?,
            val titleSuggestions: List<String>,
            val titleSuggestionProgressIndicator: Boolean = false,
            override val validationError: String? = null,
        ) : InputDialog(validationError) {
            override fun withValidationError(validationError: String?) =
                copy(validationError = validationError)
        }

        data class RecordDetails(
            val recordId: Long,
            val image: String?,
            val title: String,
            val description: String,
            val calories: Int?,
            val carbs: Float?,
            val sugar: Float?,
            val protein: Float?,
            val fat: Float?,
            val saturated: Float?,
            val salt: Float?,
            val titleSuggestions: List<String>,
            val titleSuggestionProgressIndicator: Boolean = false,
            override val validationError: String? = null,
        ) : InputDialog(validationError) {
            override fun withValidationError(validationError: String?) =
                copy(validationError = validationError)
        }

        abstract fun withValidationError(validationError: String?): InputDialog
    }

    data class SelectActionDialog(val recordId: Long): DialogState()
}

sealed class ImagePickerState {
    class ChangeImage(val recordId: Long) : ImagePickerState()
    object Create : ImagePickerState()
}
