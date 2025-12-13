package dev.gaborbiro.dailymacros.features.modal.model

import android.graphics.Bitmap
import androidx.compose.ui.text.input.TextFieldValue

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
        open val titleValidationError: String? = null,
        open val title: TextFieldValue,
        open val description: TextFieldValue,
    ) : DialogState() {

        data class CreateDialog(
            override val titleHint: String,
            override val titleValidationError: String? = null,
            override val title: TextFieldValue,
            override val description: TextFieldValue,
        ) : InputDialog(
            titleHint = titleHint,
            titleValidationError = titleValidationError,
            title = title,
            description = description,
        ) {
            override fun withTitleValidationError(titleValidationError: String?) = copy(titleValidationError = titleValidationError)
            override fun withTitle(title: TextFieldValue) = copy(title = title)
            override fun withDescription(description: TextFieldValue) = copy(description = description)
        }

        data class CreateWithImageDialog(
            val images: List<String>,
            val showProgressIndicator: Boolean = false,
            val suggestions: SummarySuggestions?,
            override val titleHint: String,
            override val titleValidationError: String? = null,
            override val title: TextFieldValue,
            override val description: TextFieldValue,
        ) : InputDialog(
            titleHint = titleHint,
            titleValidationError = titleValidationError,
            title = title,
            description = description,
        ) {
            override fun withTitleValidationError(titleValidationError: String?) = copy(titleValidationError = titleValidationError)
            override fun withTitle(title: TextFieldValue) = copy(title = title)
            override fun withDescription(description: TextFieldValue) = copy(description = description)

            data class SummarySuggestions(
                val titles: List<String>,
                val description: String?,
            )
        }

        data class RecordDetailsDialog(
            val recordId: Long,
            val images: List<String>,
            val macros: MacrosUIModel?,
            val titleSuggestions: List<String>,
            val allowEdit: Boolean,
            override val titleHint: String,
            override val titleValidationError: String? = null,
            override val title: TextFieldValue,
            override val description: TextFieldValue,
        ) : InputDialog(
            titleHint = titleHint,
            titleValidationError = titleValidationError,
            title = title,
            description = description,
        ) {
            override fun withTitleValidationError(titleValidationError: String?) = copy(titleValidationError = titleValidationError)
            override fun withTitle(title: TextFieldValue) = copy(title = title)
            override fun withDescription(description: TextFieldValue) = copy(description = description)
        }

        abstract fun withTitleValidationError(titleValidationError: String?): InputDialog
        abstract fun withTitle(title: TextFieldValue): InputDialog
        abstract fun withDescription(description: TextFieldValue): InputDialog
    }

    data class ImageInput(val type: ImageInputType) : DialogState()

    data class SelectRecordActionDialog(val recordId: Long, val title: String) : DialogState()

    data class SelectTemplateActionDialog(val templateId: Long, val title: String) : DialogState()

    data class InfoDialog(val message: String) : DialogState()
}

sealed class ImageInputType {
    data object Browse : ImageInputType()
    data object QuickPhoto : ImageInputType()
    data object TakePhoto : ImageInputType()
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
