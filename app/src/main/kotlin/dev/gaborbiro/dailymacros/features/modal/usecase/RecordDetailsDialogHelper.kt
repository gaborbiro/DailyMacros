package dev.gaborbiro.dailymacros.features.modal.usecase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.hasUnsavedEdits
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel
import dev.gaborbiro.dailymacros.features.modal.model.RecognisedFood
import dev.gaborbiro.dailymacros.features.modal.views.RecordDetailsDialogButtons
import dev.gaborbiro.dailymacros.features.modal.views.RecordDetailsDialogContent

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
    val showKeyboardOnOpen: Boolean,
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
        showKeyboardOnOpen = showKeyboardOnOpen,
        titleValidationError = dialogHandle.titleValidationError,
    )
}

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
) {
    val ui = deconstructDialogHandle(dialogHandle)
    val showKeyboardOnOpen = remember(dialogHandle) { ui.showKeyboardOnOpen }
    val viewDialog = dialogHandle as? DialogHandle.RecordDetailsDialog.View
    val showCloseOnly = viewDialog != null && !viewDialog.allowEdit
    ViewPreviewContext {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            RecordDetailsDialogContent(
                onTitleChanged = onTitleChanged,
                onDescriptionChanged = onDescriptionChanged,
                showKeyboardOnOpen = showKeyboardOnOpen,
                images = ui.images,
                showImageDeleteButton = ui.allowEdit,
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
                variantPickerOptions = viewDialog?.variantPickerOptions,
                selectedVariantTemplateId = viewDialog?.templateDbId ?: 0L,
                onVariantTemplatePicked = {},
                showQuickPickStar = viewDialog != null,
                quickPickStarred = viewDialog?.quickPickStarred == true,
                onQuickPickStarToggled = {},
            )
            when (dialogHandle) {
                is DialogHandle.RecordDetailsDialog.Edit -> {
                    RecordDetailsDialogButtons(
                        showCloseOnly = false,
                        showSaveAndAdd = false,
                        primaryEnabled = true,
                        primaryLabel = stringResource(R.string.meal_details_action_save),
                        saveAndAddLabel = null,
                        onDismissRequested = onDismissRequested,
                        onSaveTapped = onSubmitButtonTapped,
                        onSaveAndAddTapped = {},
                    )
                }

                is DialogHandle.RecordDetailsDialog.View -> {
                    val showCloseOnlyRow = showCloseOnly
                    val dirty = dialogHandle.hasUnsavedEdits()
                    when {
                        showCloseOnlyRow -> {
                            RecordDetailsDialogButtons(
                                showCloseOnly = true,
                                showSaveAndAdd = false,
                                primaryEnabled = true,
                                primaryLabel = null,
                                saveAndAddLabel = null,
                                onDismissRequested = onDismissRequested,
                                onSaveTapped = onSubmitButtonTapped,
                                onSaveAndAddTapped = {},
                            )
                        }
                        dialogHandle.openedFromTemplateDetailsOnly -> {
                            RecordDetailsDialogButtons(
                                showCloseOnly = false,
                                showSaveAndAdd = false,
                                primaryEnabled = true,
                                primaryLabel = if (dirty) {
                                    stringResource(R.string.meal_details_action_add_new_template)
                                } else {
                                    stringResource(R.string.meal_details_action_add_new)
                                },
                                saveAndAddLabel = null,
                                onDismissRequested = onDismissRequested,
                                onSaveTapped = onSubmitButtonTapped,
                                onSaveAndAddTapped = {},
                            )
                        }
                        else -> {
                            RecordDetailsDialogButtons(
                                showCloseOnly = false,
                                showSaveAndAdd = true,
                                primaryEnabled = dirty,
                                primaryLabel = if (dialogHandle.linkedRecordCountForTemplate == 1) {
                                    stringResource(R.string.meal_details_action_update)
                                } else {
                                    pluralStringResource(
                                        R.plurals.meal_details_update_linked_records,
                                        dialogHandle.linkedRecordCountForTemplate,
                                        dialogHandle.linkedRecordCountForTemplate,
                                    )
                                },
                                saveAndAddLabel = if (dirty) {
                                    stringResource(R.string.meal_details_action_add_new_template)
                                } else {
                                    stringResource(R.string.meal_details_action_add_new)
                                },
                                onDismissRequested = onDismissRequested,
                                onSaveTapped = onSubmitButtonTapped,
                                onSaveAndAddTapped = onSubmitButtonTapped,
                            )
                        }
                    }
                }
            }
        }
    }
}
