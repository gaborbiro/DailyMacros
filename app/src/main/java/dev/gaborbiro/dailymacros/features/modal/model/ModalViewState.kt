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

    sealed class RecordDetailsDialog(
        open val titleHint: String,
        open val titleValidationError: String? = null,
        open val title: TextFieldValue,
        open val description: TextFieldValue,
        open val images: List<String>,
    ) : DialogState() {

        data class Edit(
            override val title: TextFieldValue,
            override val titleHint: String,
            override val titleValidationError: String? = null,
            override val description: TextFieldValue,
            override val images: List<String>,
            val showProgressIndicator: Boolean = false,
            val suggestions: SummarySuggestions?,
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
            val macros: MacrosUIModel?,
            val allowEdit: Boolean,
            override val titleHint: String,
            override val titleValidationError: String? = null,
            override val title: TextFieldValue,
            override val description: TextFieldValue,
            override val images: List<String>,
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

    data class ImageInput(val type: ImageInputType) : DialogState()

    data class SelectRecordActionDialog(val recordId: Long, val title: String) : DialogState()

    data class SelectTemplateActionDialog(val templateId: Long, val title: String) : DialogState()

    data class InfoDialog(val message: String) : DialogState()
}

data class SummarySuggestions(
    val titles: List<String>,
    val description: String?,
)

sealed class ImageInputType {
    data object Camera : ImageInputType()
    data object BrowseImages : ImageInputType()
}

data class MacrosUIModel(
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
)
