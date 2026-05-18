package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.hasUnsavedEdits
import dev.gaborbiro.dailymacros.features.modal.model.MealVariantPickerOption
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel
import dev.gaborbiro.dailymacros.features.modal.model.RecordDetailsPristineSnapshot
import dev.gaborbiro.dailymacros.features.modal.model.RecognisedFood
import dev.gaborbiro.dailymacros.features.modal.usecase.RecordDetailsDialogPreview
import dev.gaborbiro.dailymacros.features.modal.usecase.deconstructDialogHandle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

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
    onAddImageViaCameraTapped: () -> Unit,
    onAddImageViaPickerTapped: () -> Unit,
    onDismissRequested: () -> Unit,
    onImagesInfoButtonTapped: () -> Unit,
    onRunAIButtonTapped: () -> Unit,
    onVariantTemplatePicked: (Long) -> Unit,
    onQuickPickStarToggled: () -> Unit,
) {
    val ui = deconstructDialogHandle(dialogHandle)
    val viewDialog = dialogHandle as? DialogHandle.RecordDetailsDialog.View
    val showCloseOnly = dialogHandle is DialogHandle.RecordDetailsDialog.View && !dialogHandle.allowEdit
    val showKeyboardOnOpen = remember(dialogHandle) { ui.showKeyboardOnOpen }

    ScrollableContentDialog(
        onDismissRequested = onDismissRequested,
        content = {
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
                onVariantTemplatePicked = onVariantTemplatePicked,
                showQuickPickStar = viewDialog != null,
                quickPickStarred = viewDialog?.quickPickStarred == true,
                onQuickPickStarToggled = onQuickPickStarToggled,
            )
        },
        errorMessages = errorMessages,
        buttons = {
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
                    val dirty = dialogHandle.hasUnsavedEdits()
                    when {
                        showCloseOnly -> {
                            RecordDetailsDialogButtons(
                                showCloseOnly = true,
                                showSaveAndAdd = false,
                                primaryEnabled = true,
                                primaryLabel = null,
                                saveAndAddLabel = null,
                                onDismissRequested = onDismissRequested,
                                onSaveTapped = onSaveDetailsTapped,
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
                                onSaveTapped = onSaveAndAddDetailsTapped,
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
                                onSaveTapped = onSaveDetailsTapped,
                                onSaveAndAddTapped = onSaveAndAddDetailsTapped,
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
internal fun ColumnScope.RecordDetailsDialogContent(
    onTitleChanged: (TextFieldValue) -> Unit,
    onDescriptionChanged: (TextFieldValue) -> Unit,
    showKeyboardOnOpen: Boolean,
    images: List<String>,
    showImageDeleteButton: Boolean,
    title: TextFieldValue,
    showRunAIButton: Boolean,
    titleHint: String,
    analysis: RecognisedFood?,
    showProgressIndicator: Boolean,
    description: TextFieldValue,
    titleErrorMessage: String?,
    allowEdit: Boolean,
    nutrientBreakdown: NutrientBreakdownUiModel?,
    onImageTapped: (String) -> Unit,
    onImageDeleteTapped: (String) -> Unit,
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
) {
    val focusRequester = remember { FocusRequester() }

    if (showKeyboardOnOpen && variantPickerOptions.isNullOrEmpty()) {
        LaunchedEffect(key1 = Unit) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    ImageStrip(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PaddingDefault)
            .padding(bottom = PaddingDefault),
        showAddPhotoButtons = allowEdit,
        showImageDeleteButton = showImageDeleteButton,
        images = images,
        onImageTapped = onImageTapped,
        onImageDeleteTapped = onImageDeleteTapped,
        onAddImageViaCameraTapped = onAddImageViaCameraTapped,
        onAddImageViaPickerTapped = onAddImageViaPickerTapped,
        onInfoButtonTapped = onImagesInfoButtonTapped,
    )

    if (!variantPickerOptions.isNullOrEmpty()) {
        VariantTemplateDropdown(
            options = variantPickerOptions,
            selectedTemplateId = selectedVariantTemplateId,
            titleHint = titleHint,
            titleErrorMessage = titleErrorMessage,
            onTemplatePicked = onVariantTemplatePicked,
        )
    }

    if (showQuickPickStar) {
        QuickPickStarRow(
            starred = quickPickStarred,
            onToggle = onQuickPickStarToggled,
        )
    }

    TextField(
        modifier = Modifier
            .padding(horizontal = PaddingDefault)
            .fillMaxWidth()
            .wrapContentHeight()
            .focusRequester(focusRequester),
        enabled = allowEdit,
        isError = titleErrorMessage.isNullOrBlank().not(),
        textStyle = MaterialTheme.typography.bodyMedium,
        trailingIcon = {
                if (showProgressIndicator) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(25.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                } else {
                    if (showRunAIButton) {
                        Button(
                            modifier = Modifier
                                .size(48.dp)
                                .padding(PaddingHalf),
                            contentPadding = PaddingValues(),
                            onClick = onRunAIButtonTapped,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chatgpt),
                                contentDescription = "Run image recognition"
                            )
                        }
                    }
                }
            },
            placeholder = {
                if (allowEdit) {
                    Text(
                        text = titleHint,
                        style = MaterialTheme.typography.labelLarge,
                        fontStyle = FontStyle.Italic,
                        color = Color.Gray,
                    )
                }
            },
            value = title,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            onValueChange = {
                onTitleChanged(it)
            },
        )
    if (titleErrorMessage.isNullOrBlank().not()) {
        Text(
            text = titleErrorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .height(16.dp)
                .padding(horizontal = PaddingDefault),
        )
    }

    analysis
        ?.title
        ?.let {
            Spacer(
                modifier = Modifier
                    .height(PaddingHalf)
            )
        }

    Spacer(
        modifier = Modifier
            .height(PaddingDefault)
    )

    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaddingDefault)
            .let {
                if (allowEdit) {
                    it.height(96.dp)
                } else {
                    it.wrapContentHeight()
                }
            },
        enabled = allowEdit,
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = {
            if (allowEdit) {
                Text(
                    text = "Mention unclear components, quantities or other instructions for the AI (for ex. \"I only ate half of it\")",
                    style = MaterialTheme.typography.labelLarge,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray,
                )
            }
        },
        value = description,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences,
        ),
        onValueChange = {
            onDescriptionChanged(it)
        },
    )

    analysis
        ?.description
        ?.takeIf { it.isNotBlank() }
        ?.let { description ->
            Column {
                Row(
                    modifier = Modifier
                        .padding(horizontal = PaddingDefault)
                        .padding(top = PaddingDefault),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .alpha(.3f),
                        painter = painterResource(R.drawable.ic_chatgpt),
                        contentDescription = "chatgpt",
                    )
                    Text(
                        text = "analysis:",
                        style = MaterialTheme.typography.labelLarge,
                        fontStyle = FontStyle.Italic,
                        color = Color.Gray,
                    )
                }
                OutlinedText(
                    modifier = Modifier
                        .padding(horizontal = PaddingDefault),
                    text = description,
                )
            }
        }

    nutrientBreakdown?.let {
        Spacer(
            modifier = Modifier
                .height(PaddingDefault)
        )

        NutrientsIndentedList(nutrientBreakdown = nutrientBreakdown)
    }
}

@Composable
private fun QuickPickStarRow(
    starred: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = PaddingDefault)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (starred) {
                    stringResource(R.string.meal_details_quick_pick_unstar_cd)
                } else {
                    stringResource(R.string.meal_details_quick_pick_star_cd)
                },
                tint = if (starred) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VariantTemplateDropdown(
    options: List<MealVariantPickerOption>,
    selectedTemplateId: Long,
    titleHint: String,
    titleErrorMessage: String?,
    onTemplatePicked: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.find { it.templateId == selectedTemplateId } ?: options.firstOrNull()
    val summaryForField = selected?.let { "${it.title}\n${it.lastUsedDateLabel}" } ?: ""
    val pickVariantLabel = stringResource(R.string.meal_details_pick_variant_cd)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .padding(horizontal = PaddingDefault)
            .fillMaxWidth()
            .semantics { contentDescription = pickVariantLabel },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            value = summaryForField,
            onValueChange = {},
            isError = titleErrorMessage != null && titleErrorMessage.isNotBlank(),
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = {
                Text(
                    text = titleHint,
                    style = MaterialTheme.typography.labelLarge,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray,
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                if (index == 1) {
                    HorizontalDivider()
                }
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = option.lastUsedDateLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onTemplatePicked(option.templateId)
                    },
                )
            }
        }
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
        ),
    )
}
