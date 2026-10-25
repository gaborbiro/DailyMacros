package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.R as CommonR
import dev.gaborbiro.dailymacros.features.modal.R
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.MealVariantPickerOption
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel
import dev.gaborbiro.dailymacros.features.modal.model.RecognisedFood
import dev.gaborbiro.dailymacros.features.modal.model.RecordDetailsPristineSnapshot
import dev.gaborbiro.dailymacros.features.modal.model.recordDetailsEditPristineSnapshot
import dev.gaborbiro.dailymacros.features.modal.usecase.RecordDetailsDialogPreview
import dev.gaborbiro.dailymacros.features.modal.usecase.deconstructDialogHandle
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel
import kotlinx.coroutines.flow.Flow
import java.time.ZoneId
import java.time.ZonedDateTime

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
    onEditTimestampTapped: () -> Unit,
    onCorrectionChanged: (TextFieldValue) -> Unit,
    onReanalyzeTapped: () -> Unit,
    onTitleSpoken: (String) -> Unit,
    onDescriptionSpoken: (String) -> Unit,
    onCorrectionSpoken: (String) -> Unit,
    onVoiceInputFailed: (Throwable) -> Unit,
) {
    val ui = deconstructDialogHandle(dialogHandle)
    val viewDialog = dialogHandle as? DialogHandle.RecordDetailsDialog.View
    val showCloseOnly = dialogHandle is DialogHandle.RecordDetailsDialog.View && !dialogHandle.allowEdit
    val showKeyboardOnOpen = remember(dialogHandle) { ui.showKeyboardOnOpen }
    val showPhotoManagement = when (dialogHandle) {
        is DialogHandle.RecordDetailsDialog.Edit -> true
        is DialogHandle.RecordDetailsDialog.View ->
            dialogHandle.isEditing && dialogHandle.allowEdit && !showCloseOnly
    }

    var pendingDiscard by remember { mutableStateOf<PendingRecordDetailsDiscard?>(null) }

    fun requestDismissRecordDetails() {
        if (dialogHandle.hasUnsavedEdits) {
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
                imageFilenames = ui.imageFilenames,
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
                onEditTimestampTapped = onEditTimestampTapped,
                onCorrectionChanged = onCorrectionChanged,
                onReanalyzeTapped = onReanalyzeTapped,
                onTitleSpoken = onTitleSpoken,
                onDescriptionSpoken = onDescriptionSpoken,
                onCorrectionSpoken = onCorrectionSpoken,
                onVoiceInputFailed = onVoiceInputFailed,
            )
        },
        errorMessages = errorMessages,
        topEndAction = if (viewDialog != null && !showCloseOnly) {
            {
                RecordDetailsHeaderActions(
                    visible = !viewDialog.isEditing,
                    quickPickStarred = viewDialog.quickPickStarred,
                    onQuickPickStarToggled = onQuickPickStarToggled,
                    // A quick pick's details already lead with "Log meal again".
                    showLogMealAgain = !viewDialog.openedFromTemplateDetailsOnly,
                    onEditTapped = onRecordDetailsEditStarted,
                    onLogMealAgainTapped = onSaveAndAddDetailsTapped,
                )
            }
        } else null,
        footer = {
            when (dialogHandle) {
                is DialogHandle.RecordDetailsDialog.Edit -> {
                    RecordDetailsDialogButtons(
                        showCloseOnly = false,
                        showSaveAndAdd = false,
                        primaryEnabled = true,
                        primaryLabel = stringResource(R.string.meal_details_action_save_and_analyze),
                        saveAndAddLabel = null,
                        onDismissRequested = { requestDismissRecordDetails() },
                        onSaveTapped = onSubmitButtonTapped,
                        onSaveAndAddTapped = {},
                        primaryStartsAnalysis = true,
                    )
                }

                is DialogHandle.RecordDetailsDialog.View -> {
                    if (showCloseOnly) {
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
                    } else {
                        AnimatedContent(
                            targetState = dialogHandle.isEditing,
                            transitionSpec = {
                                fadeIn(editModeSpec()) togetherWith fadeOut(editModeSpec()) using
                                        SizeTransform(clip = false) { _, _ -> editModeSpec() }
                            },
                            label = "mealDetailsFooter",
                        ) { editing ->
                            if (editing) {
                                RecordDetailsViewEditButtons(
                                    primaryEnabled = dialogHandle.hasUnsavedEdits,
                                    primaryLabel = updateButtonLabel(dialogHandle),
                                    primaryStartsAnalysis = dialogHandle.editsRequireAnalysis,
                                    onUpdate = onSaveDetailsTapped,
                                    onSaveAndAdd = onSaveAndAddDetailsTapped,
                                    onCancel = {
                                        if (dialogHandle.hasUnsavedEdits) {
                                            pendingDiscard = PendingRecordDetailsDiscard.ExitEditMode
                                        } else {
                                            onRecordDetailsEditCancelled()
                                        }
                                    },
                                )
                            } else {
                                RecordDetailsViewBrowseButtons(
                                    showLogMealAgain = dialogHandle.openedFromTemplateDetailsOnly,
                                    onLogMealAgain = onSaveAndAddDetailsTapped,
                                    onDismissRequested = { requestDismissRecordDetails() },
                                )
                            }
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
private fun updateButtonLabel(dialogHandle: DialogHandle.RecordDetailsDialog.View): String {
    val linkedCount = dialogHandle.linkedRecordCountForTemplate
    return when {
        linkedCount == 1 && dialogHandle.editsRequireAnalysis ->
            stringResource(R.string.meal_details_action_update_and_analyze)

        linkedCount == 1 -> stringResource(R.string.meal_details_action_update)

        dialogHandle.editsRequireAnalysis ->
            pluralStringResource(R.plurals.meal_details_update_and_analyze_linked_records, linkedCount, linkedCount)

        else -> pluralStringResource(R.plurals.meal_details_update_linked_records, linkedCount, linkedCount)
    }
}

/**
 * The Quick Pick star and an overflow menu. Editing and logging the meal again are things the user does
 * later, if at all, so they sit behind the menu instead of competing with reading the results.
 */
@Composable
private fun RecordDetailsHeaderActions(
    visible: Boolean,
    quickPickStarred: Boolean,
    onQuickPickStarToggled: () -> Unit,
    showLogMealAgain: Boolean,
    onEditTapped: () -> Unit,
    onLogMealAgainTapped: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(editModeSpec()),
        exit = fadeOut(editModeSpec()),
    ) {
        Row(modifier = Modifier.padding(top = 12.dp, end = PaddingHalf)) {
            IconButton(onClick = onQuickPickStarToggled) {
                Icon(
                    imageVector = if (quickPickStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (quickPickStarred) {
                        stringResource(R.string.meal_details_quick_pick_remove_cd)
                    } else {
                        stringResource(R.string.meal_details_quick_pick_add_cd)
                    },
                    tint = if (quickPickStarred) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.meal_details_menu_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.meal_details_menu_edit)) },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEditTapped()
                        },
                    )
                    if (showLogMealAgain) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.meal_details_action_add_new)) },
                            leadingIcon = { Icon(painter = painterResource(CommonR.drawable.ic_exposure_plus_1), contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onLogMealAgainTapped()
                            },
                        )
                    }
                }
            }
        }
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
    imageFilenames: List<String>,
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
    onEditTimestampTapped: () -> Unit = { },
    onCorrectionChanged: (TextFieldValue) -> Unit = { },
    onReanalyzeTapped: () -> Unit = { },
    onTitleSpoken: (String) -> Unit = { },
    onDescriptionSpoken: (String) -> Unit = { },
    onCorrectionSpoken: (String) -> Unit = { },
    onVoiceInputFailed: (Throwable) -> Unit = { },
) {
    when (dialogHandle) {
        is DialogHandle.RecordDetailsDialog.Edit ->
            RecordDetailsCreateView(
                onTitleChanged = onTitleChanged,
                onDescriptionChanged = onDescriptionChanged,
                showKeyboardOnOpen = showKeyboardOnOpen,
                imageFilenames = imageFilenames,
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
                onTitleSpoken = onTitleSpoken,
                onDescriptionSpoken = onDescriptionSpoken,
                onVoiceInputFailed = onVoiceInputFailed,
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
                imageFilenames = imageFilenames,
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
                onEditTimestampTapped = onEditTimestampTapped,
                onCorrectionChanged = onCorrectionChanged,
                onReanalyzeTapped = onReanalyzeTapped,
                onTitleSpoken = onTitleSpoken,
                onDescriptionSpoken = onDescriptionSpoken,
                onCorrectionSpoken = onCorrectionSpoken,
                onVoiceInputFailed = onVoiceInputFailed,
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
            titleHint = "Title (optional)",
            description = TextFieldValue("I ate an apple"),
            imageFilenames = listOf("1", "2"),
            allowEdit = true,
            nutrientBreakdown = NutrientBreakdownUiModel(
                nutrients = previewNutrientRows(),
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
                imageFilenames = listOf("1", "2"),
            ),
            linkedRecordCountForTemplate = 3,
            timestamp = ZonedDateTime.now(ZoneId.systemDefault()),
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
            titleHint = "Title (optional)",
            description = TextFieldValue(),
            imageFilenames = listOf("1", "2"),
            recognisedFood = RecognisedFood(
                title = "This is a title suggestion",
                warning = null,
            ),
            pristineSnapshot = recordDetailsEditPristineSnapshot(
                title = TextFieldValue(),
                description = TextFieldValue(),
                imageFilenames = listOf("1", "2"),
            ),
            startedAt = ZonedDateTime.now(ZoneId.systemDefault()),
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
            imageFilenames = listOf("1", "2"),
            showProgressIndicator = true,
            recognisedFood = null,
            pristineSnapshot = recordDetailsEditPristineSnapshot(
                title = TextFieldValue(),
                description = TextFieldValue(),
                imageFilenames = listOf("1", "2"),
            ),
            startedAt = ZonedDateTime.now(ZoneId.systemDefault()),
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
            titleHint = "Title (optional)",
            titleValidationError = "error",
            description = TextFieldValue(),
            imageFilenames = listOf("1", "2"),
            recognisedFood = RecognisedFood(
                title = "This is a title suggestion",
                warning = null,
            ),
            pristineSnapshot = recordDetailsEditPristineSnapshot(
                title = TextFieldValue(),
                description = TextFieldValue(),
                imageFilenames = listOf("1", "2"),
            ),
            startedAt = ZonedDateTime.now(ZoneId.systemDefault()),
        ),
    )
}
