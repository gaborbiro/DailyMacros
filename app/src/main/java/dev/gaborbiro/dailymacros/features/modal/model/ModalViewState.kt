package dev.gaborbiro.dailymacros.features.modal.model

import androidx.compose.ui.text.input.TextFieldValue

data class ModalViewState(
    val rootDialog: DialogHandle? = null,
    val overlayDialog: DialogHandle? = null,
    val close: Boolean = false,
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
            val nutrientBreakdown: NutrientBreakdownUiModel?,
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

    data class ImageInput(val type: ImageInputType) : DialogHandle()

    data class SelectRecordActionDialog(val recordId: Long, val title: String) : DialogHandle()

    data class SelectTemplateActionDialog(val templateId: Long, val title: String) : DialogHandle()

    data class InfoDialog(val message: String) : DialogHandle()
}

data class RecognisedFood(
    val title: String?,
    val description: String?,
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
)
