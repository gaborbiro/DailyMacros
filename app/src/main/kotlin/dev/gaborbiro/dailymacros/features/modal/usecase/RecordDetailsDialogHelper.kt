package dev.gaborbiro.dailymacros.features.modal.usecase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel
import dev.gaborbiro.dailymacros.features.modal.model.RecognisedFood
import dev.gaborbiro.dailymacros.features.modal.model.VariabilityArchetypePickerEntry
import dev.gaborbiro.dailymacros.features.modal.views.RecordDetailsDialogButtons
import dev.gaborbiro.dailymacros.features.modal.views.RecordDetailsDialogContent
import dev.gaborbiro.dailymacros.features.modal.views.ScrollableContentDialog

internal data class DeconstructedDialogHandle(
    val title: TextFieldValue,
    val titleHint: String,
    val showRunAIButton: Boolean,
    val images: List<String>,
    val analysis: RecognisedFood?,
    val showProgressIndicator: Boolean,
    val description: TextFieldValue,
    val nutrientBreakdown: NutrientBreakdownUiModel?,
    val allowEdit: Boolean,
    val variabilityArchetypePickerEntries: List<VariabilityArchetypePickerEntry>,
    val saveButtonText: String,
    val showKeyboardOnOpen: Boolean,
    val showVariabilityDifferentMealLink: Boolean,
    val titleValidationError: String?,
)

internal fun deconstructDialogHandle(
    dialogHandle: DialogHandle.RecordDetailsDialog,
): DeconstructedDialogHandle {
    val showRunAIButton = (dialogHandle as? DialogHandle.RecordDetailsDialog.Edit)
        ?.showRunAIButton
        ?: false
    val analysis = (dialogHandle as? DialogHandle.RecordDetailsDialog.Edit)
        ?.recognisedFood
    val showProgressIndicator = (dialogHandle as? DialogHandle.RecordDetailsDialog.Edit)
        ?.showProgressIndicator
    val nutrientBreakdown = (dialogHandle as? DialogHandle.RecordDetailsDialog.View)
        ?.nutrientBreakdown
    val allowEdit = (dialogHandle as? DialogHandle.RecordDetailsDialog.View)
        ?.allowEdit
        ?: true
    val variabilityArchetypePickerEntries =
        (dialogHandle as? DialogHandle.RecordDetailsDialog.View)?.variabilityArchetypePickerEntries
            ?: emptyList()
    val saveButtonText =
        if (dialogHandle is DialogHandle.RecordDetailsDialog.View) "Update and Analyze" else "Add and Analyze"
    val showKeyboardOnOpen = when (dialogHandle) {
        is DialogHandle.RecordDetailsDialog.Edit -> true
        is DialogHandle.RecordDetailsDialog.View -> false
    }
    return DeconstructedDialogHandle(
        title = dialogHandle.title,
        titleHint = dialogHandle.titleHint,
        showRunAIButton = showRunAIButton,
        images = dialogHandle.images,
        analysis = analysis,
        showProgressIndicator = showProgressIndicator ?: false,
        description = dialogHandle.description,
        nutrientBreakdown = nutrientBreakdown,
        allowEdit = allowEdit,
        variabilityArchetypePickerEntries = variabilityArchetypePickerEntries,
        saveButtonText = saveButtonText,
        showKeyboardOnOpen = showKeyboardOnOpen,
        showVariabilityDifferentMealLink = dialogHandle.showVariabilityDifferentMealLink,
        titleValidationError = dialogHandle.titleValidationError,
    )
}

/**
 * Studio layout preview only: avoids [ScrollableContentDialog] (Dialog + nested measure) which can trip
 * layoutlib’s cooperative interrupt loop breaker.
 */
@Composable
internal fun RecordDetailsDialogPreview(
    dialogHandle: DialogHandle.RecordDetailsDialog,
    onTitleChanged: (TextFieldValue) -> Unit = {},
    onDescriptionChanged: (TextFieldValue) -> Unit = {},
    onSubmitButtonTapped: () -> Unit = {},
    onImageTapped: (String) -> Unit = {},
    onImageDeleteTapped: (String) -> Unit = {},
    onAddImageViaCameraTapped: () -> Unit = {},
    onAddImageViaPickerTapped: () -> Unit = {},
    onDismissRequested: () -> Unit = {},
    onImagesInfoButtonTapped: () -> Unit = {},
    onRunAIButtonTapped: () -> Unit = {},
    onVariabilityDifferentMealLinkTapped: (archetypeKey: String) -> Unit = { _ -> },
) {
    val ui = deconstructDialogHandle(dialogHandle)
    val showKeyboardOnOpen = remember(dialogHandle) { ui.showKeyboardOnOpen }
    ViewPreviewContext {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            RecordDetailsDialogContent(
                onTitleChanged = onTitleChanged,
                onDescriptionChanged = onDescriptionChanged,
                showKeyboardOnOpen = showKeyboardOnOpen,
                images = ui.images,
                title = ui.title,
                showRunAIButton = ui.showRunAIButton,
                titleHint = ui.titleHint,
                analysis = ui.analysis,
                showProgressIndicator = ui.showProgressIndicator,
                description = ui.description,
                titleErrorMessage = ui.titleValidationError,
                allowEdit = ui.allowEdit,
                nutrientBreakdown = ui.nutrientBreakdown,
                onImageTapped = onImageTapped,
                onImageDeleteTapped = onImageDeleteTapped,
                onAddImageViaCameraTapped = onAddImageViaCameraTapped,
                onAddImageViaPickerTapped = onAddImageViaPickerTapped,
                onImagesInfoButtonTapped = onImagesInfoButtonTapped,
                onRunAIButtonTapped = onRunAIButtonTapped,
                showVariabilityDifferentMealLink = ui.showVariabilityDifferentMealLink,
                variabilityArchetypePickerEntries = ui.variabilityArchetypePickerEntries,
                onVariabilityDifferentMealLinkTapped = onVariabilityDifferentMealLinkTapped,
            )
            RecordDetailsDialogButtons(
                allowEdit = ui.allowEdit,
                saveButtonText = ui.saveButtonText,
                onDismissRequested = onDismissRequested,
                onSubmitButtonTapped = onSubmitButtonTapped,
            )
        }
    }
}
