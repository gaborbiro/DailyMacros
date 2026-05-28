package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.MealVariantPickerOption
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel
import dev.gaborbiro.dailymacros.features.modal.model.RecognisedFood
import dev.gaborbiro.dailymacros.features.modal.model.RecordDetailsPristineSnapshot
import dev.gaborbiro.dailymacros.features.modal.model.hasUnsavedEdits
import dev.gaborbiro.dailymacros.features.modal.model.recordDetailsEditPristineSnapshot
import dev.gaborbiro.dailymacros.features.modal.usecase.RecordDetailsDialogPreview
import dev.gaborbiro.dailymacros.features.modal.usecase.deconstructDialogHandle
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel
import kotlinx.coroutines.flow.Flow

private sealed class PendingRecordDetailsDiscard {
    data object CloseDialog : PendingRecordDetailsDiscard()
    data object ExitEditMode : PendingRecordDetailsDiscard()
}

@Composable
internal fun RecordDetailsDialog(
    dialogHandle: DialogHandle.RecordDetailsDialog,
    errorMessages: Flow<String>,
    onTitleChanged: (TextFieldValue) -> Unit,
    onDescriptionChanged: (TextFieldValue) -> Unit,
    onSubmitButtonTapped: () -> Unit,
    onSaveDetailsTapped: () -> Unit,
    onSaveAndAddDetailsTapped: () -> Unit,
    onImageTapped: (String) -> Unit,
    onImageDeleteTapped: (String) -> Unit,
    onImageMoveLeftTapped: (String) -> Unit,
    onImageMoveRightTapped: (String) -> Unit,
    onAddImageViaCameraTapped: () -> Unit,
    onAddImageViaPickerTapped: () -> Unit,
    onDismissRequested: () -> Unit,
    onImagesInfoButtonTapped: () -> Unit,
    onRunAIButtonTapped: () -> Unit,
    onVariantTemplatePicked: (Long) -> Unit,
    onQuickPickStarToggled: () -> Unit,
    onRecordDetailsEditStarted: () -> Unit,
    onRecordDetailsEditCancelled: () -> Unit,
) {
    val ui = deconstructDialogHandle(dialogHandle)
    val viewDialog = dialogHandle as? DialogHandle.RecordDetailsDialog.View
    val showCloseOnly = dialogHandle is DialogHandle.RecordDetailsDialog.View && !dialogHandle.allowEdit
    val showKeyboardOnOpen = remember(dialogHandle) { ui.showKeyboardOnOpen }
    val showPhotoManagement = when (dialogHandle) {
        is DialogHandle.RecordDetailsDialog.Edit -> true
        is DialogHandle.RecordDetailsDialog.View ->
            dialogHandle.isEditing && dialogHandle.allowEdit
    }

    var pendingDiscard by remember { mutableStateOf<PendingRecordDetailsDiscard?>(null) }

    fun requestDismissRecordDetails() {
        if (dialogHandle.hasUnsavedEdits()) {
            pendingDiscard = PendingRecordDetailsDiscard.CloseDialog
        } else {
            onDismissRequested()
        }
    }

    ScrollableContentDialog(
        onDismissRequested = { requestDismissRecordDetails() },
        content = {
            RecordDetailsDialogContent(
                dialogHandle = dialogHandle,
                onTitleChanged = onTitleChanged,
                onDescriptionChanged = onDescriptionChanged,
                showKeyboardOnOpen = showKeyboardOnOpen,
                showCloseOnly = showCloseOnly,
                showPhotoManagement = showPhotoManagement,
                images = ui.images,
                title = ui.title,
                showRunAIButton = ui.showRunAIButton,
                titleHint = ui.titleHint,
                showProgressIndicator = ui.showProgressIndicator,
                description = ui.description,
                titleErrorMessage = ui.titleValidationError,
                nutrientBreakdown = ui.nutrientBreakdown,
                onImageTapped = onImageTapped,
                onImageDeleteTapped = onImageDeleteTapped,
                onImageMoveLeftTapped = onImageMoveLeftTapped,
                onImageMoveRightTapped = onImageMoveRightTapped,
                onAddImageViaCameraTapped = onAddImageViaCameraTapped,
                onAddImageViaPickerTapped = onAddImageViaPickerTapped,
                onImagesInfoButtonTapped = onImagesInfoButtonTapped,
                onRunAIButtonTapped = onRunAIButtonTapped,
                variantPickerOptions = viewDialog?.variantPickerOptions,
                selectedVariantTemplateId = viewDialog?.templateDbId ?: 0L,
                onVariantTemplatePicked = onVariantTemplatePicked,
                showQuickPickStar = viewDialog != null && viewDialog.allowEdit,
                quickPickStarred = viewDialog?.quickPickStarred == true,
                onQuickPickStarToggled = onQuickPickStarToggled,
                onBeginViewEdit = onRecordDetailsEditStarted,
            )
        },
        errorMessages = errorMessages,
        footer = {
            when (dialogHandle) {
                is DialogHandle.RecordDetailsDialog.Edit -> {
                    RecordDetailsDialogButtons(
                        showCloseOnly = false,
                        showSaveAndAdd = false,
                        primaryEnabled = true,
                        primaryLabel = stringResource(R.string.meal_details_action_save),
                        saveAndAddLabel = null,
                        onDismissRequested = { requestDismissRecordDetails() },
                        onSaveTapped = onSubmitButtonTapped,
                        onSaveAndAddTapped = {},
                    )
                }

                is DialogHandle.RecordDetailsDialog.View -> {
                    val dirty = dialogHandle.hasUnsavedEdits()
                    when {
                        showCloseOnly -> {
                            RecordDetailsDialogButtons(
                                showCloseOnly = true,
                                showSaveAndAdd = false,
                                primaryEnabled = true,
                                primaryLabel = null,
                                saveAndAddLabel = null,
                                onDismissRequested = { requestDismissRecordDetails() },
                                onSaveTapped = {},
                                onSaveAndAddTapped = {},
                            )
                        }

                        dialogHandle.isEditing -> {
                            RecordDetailsViewEditButtons(
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
                                onUpdate = onSaveDetailsTapped,
                                onSaveAndAdd = onSaveAndAddDetailsTapped,
                                onCancel = {
                                    if (dialogHandle.hasUnsavedEdits()) {
                                        pendingDiscard = PendingRecordDetailsDiscard.ExitEditMode
                                    } else {
                                        onRecordDetailsEditCancelled()
                                    }
                                },
                            )
                        }

                        else -> {
                            RecordDetailsViewBrowseButtons(
                                onLogMealAgain = onSaveAndAddDetailsTapped,
                                onDismissRequested = { requestDismissRecordDetails() },
                            )
                        }
                    }
                }
            }
        },
    )

    if (pendingDiscard != null) {
        RecordDetailsCloseUnsavedDialog(
            onDiscard = {
                val pending = pendingDiscard ?: return@RecordDetailsCloseUnsavedDialog
                pendingDiscard = null
                when (pending) {
                    PendingRecordDetailsDiscard.CloseDialog -> onDismissRequested()
                    PendingRecordDetailsDiscard.ExitEditMode -> onRecordDetailsEditCancelled()
                }
            },
            onKeepEditing = { pendingDiscard = null },
        )
    }
}

@Composable
internal fun ColumnScope.RecordDetailsDialogContent(
    dialogHandle: DialogHandle.RecordDetailsDialog,
    onTitleChanged: (TextFieldValue) -> Unit,
    onDescriptionChanged: (TextFieldValue) -> Unit,
    showKeyboardOnOpen: Boolean,
    showCloseOnly: Boolean,
    showPhotoManagement: Boolean,
    images: List<String>,
    title: TextFieldValue,
    showRunAIButton: Boolean,
    titleHint: String,
    showProgressIndicator: Boolean,
    description: TextFieldValue,
    titleErrorMessage: String?,
    nutrientBreakdown: NutrientBreakdownUiModel?,
    onImageTapped: (String) -> Unit,
    onImageDeleteTapped: (String) -> Unit,
    onImageMoveLeftTapped: (String) -> Unit,
    onImageMoveRightTapped: (String) -> Unit,
    onAddImageViaCameraTapped: () -> Unit,
    onAddImageViaPickerTapped: () -> Unit,
    onImagesInfoButtonTapped: () -> Unit,
    onRunAIButtonTapped: () -> Unit,
    variantPickerOptions: List<MealVariantPickerOption>? = null,
    selectedVariantTemplateId: Long = 0L,
    onVariantTemplatePicked: (Long) -> Unit = { },
    showQuickPickStar: Boolean = false,
    quickPickStarred: Boolean = false,
    onQuickPickStarToggled: () -> Unit = { },
    onBeginViewEdit: () -> Unit = { },
) {
    when (dialogHandle) {
        is DialogHandle.RecordDetailsDialog.Edit ->
            RecordDetailsCreateView(
                onTitleChanged = onTitleChanged,
                onDescriptionChanged = onDescriptionChanged,
                showKeyboardOnOpen = showKeyboardOnOpen,
                images = images,
                title = title,
                showRunAIButton = showRunAIButton,
                titleHint = titleHint,
                showProgressIndicator = showProgressIndicator,
                description = description,
                titleErrorMessage = titleErrorMessage,
                onImageTapped = onImageTapped,
                onImageDeleteTapped = onImageDeleteTapped,
                onImageMoveLeftTapped = onImageMoveLeftTapped,
                onImageMoveRightTapped = onImageMoveRightTapped,
                onAddImageViaCameraTapped = onAddImageViaCameraTapped,
                onAddImageViaPickerTapped = onAddImageViaPickerTapped,
                onImagesInfoButtonTapped = onImagesInfoButtonTapped,
                onRunAIButtonTapped = onRunAIButtonTapped,
            )

        is DialogHandle.RecordDetailsDialog.View ->
            RecordDetailsView(
                view = dialogHandle,
                showCloseOnly = showCloseOnly,
                showPhotoManagement = showPhotoManagement,
                onTitleChanged = onTitleChanged,
                onDescriptionChanged = onDescriptionChanged,
                title = title,
                titleHint = titleHint,
                titleErrorMessage = titleErrorMessage,
                description = description,
                nutrientBreakdown = nutrientBreakdown,
                images = images,
                onImageTapped = onImageTapped,
                onImageDeleteTapped = onImageDeleteTapped,
                onImageMoveLeftTapped = onImageMoveLeftTapped,
                onImageMoveRightTapped = onImageMoveRightTapped,
                onAddImageViaCameraTapped = onAddImageViaCameraTapped,
                onAddImageViaPickerTapped = onAddImageViaPickerTapped,
                onImagesInfoButtonTapped = onImagesInfoButtonTapped,
                variantPickerOptions = variantPickerOptions,
                selectedVariantTemplateId = selectedVariantTemplateId,
                onVariantTemplatePicked = onVariantTemplatePicked,
                showQuickPickStar = showQuickPickStar,
                quickPickStarred = quickPickStarred,
                onQuickPickStarToggled = onQuickPickStarToggled,
                onBeginViewEdit = onBeginViewEdit,
            )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreviewView() {
    RecordDetailsDialogPreview(
        dialogHandle = DialogHandle.RecordDetailsDialog.View(
            recordId = 1L,
            templateDbId = 1L,
            variabilityAnchorTemplateDbId = 1L,
            title = TextFieldValue("Apple"),
            titleHint = "Give your meal a title",
            description = TextFieldValue("I ate an apple"),
            images = listOf("1", "2"),
            allowEdit = true,
            nutrientBreakdown = NutrientBreakdownUiModel(
                calories = "Calories: 2100 cal",
                protein = "Protein: 150g",
                fat = "Fat 100g",
                ofWhichSaturated = "of which saturated: 20g",
                carbs = "Carbs: 100g",
                ofWhichSugar = "of which sugar: 30g",
                ofWhichAddedSugar = "of which added sugar: 15g",
                salt = "Salt: 5g",
                fibre = "Fibre: 4.5g",
                notes = "Notes: This is a note",
            ),
            compactNutrients = NutrientsUiModel(
                calories = "2100kcal",
                protein = "Protein 150g",
                fat = "Fat 100g",
                carbs = "Carbs 100g",
                salt = "Salt 5g",
                fibre = "Fibre 4.5g",
            ),
            pristineSnapshot = RecordDetailsPristineSnapshot(
                templateDbId = 1L,
                title = "Apple",
                description = "I ate an apple",
                images = listOf("1", "2"),
            ),
            linkedRecordCountForTemplate = 3,
        ),
    )
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreviewSuggestion() {
    RecordDetailsDialogPreview(
        dialogHandle = DialogHandle.RecordDetailsDialog.Edit(
            title = TextFieldValue(),
            titleHint = "Give your meal a title",
            description = TextFieldValue(),
            images = listOf("1", "2"),
            recognisedFood = RecognisedFood(
                title = "This is a title suggestion",
                description = "This ready meal contains curry of beef (caril de vitela), basmati rice, leeks, and carrots. It is labeled as medium size (250g) and high in carbohydrates. The dish also contains tomato pulp, onion, olive oil, curry spice blend, celery, turmeric, and salt.",
            ),
            pristineSnapshot = recordDetailsEditPristineSnapshot(
                title = TextFieldValue(),
                description = TextFieldValue(),
                images = listOf("1", "2"),
            ),
        ),
    )
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreview() {
    RecordDetailsDialogPreview(
        dialogHandle = DialogHandle.RecordDetailsDialog.Edit(
            title = TextFieldValue(),
            titleHint = "What did you eat?",
            description = TextFieldValue(),
            images = listOf("1", "2"),
            showProgressIndicator = true,
            recognisedFood = null,
            pristineSnapshot = recordDetailsEditPristineSnapshot(
                title = TextFieldValue(),
                description = TextFieldValue(),
                images = listOf("1", "2"),
            ),
        ),
    )
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreviewError() {
    RecordDetailsDialogPreview(
        dialogHandle = DialogHandle.RecordDetailsDialog.Edit(
            title = TextFieldValue(),
            titleHint = "Give your meal a title",
            titleValidationError = "error",
            description = TextFieldValue(),
            images = listOf("1", "2"),
            recognisedFood = RecognisedFood(
                title = "This is a title suggestion",
                description = "This ready meal contains curry of beef (caril de vitela), basmati rice, leeks, and carrots. It is labeled as medium size (250g) and high in carbohydrates. The dish also contains tomato pulp, onion, olive oil, curry spice blend, celery, turmeric, and salt.",
            ),
            pristineSnapshot = recordDetailsEditPristineSnapshot(
                title = TextFieldValue(),
                description = TextFieldValue(),
                images = listOf("1", "2"),
            ),
        ),
    )
}
