package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import dev.gaborbiro.dailymacros.features.modal.model.MealVariantPickerOption
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel
import dev.gaborbiro.dailymacros.features.modal.model.RecordDetailsPristineSnapshot
import dev.gaborbiro.dailymacros.features.modal.model.RecognisedFood
import dev.gaborbiro.dailymacros.features.modal.model.hasUnsavedEdits
import dev.gaborbiro.dailymacros.features.modal.usecase.RecordDetailsDialogPreview
import dev.gaborbiro.dailymacros.features.modal.usecase.deconstructDialogHandle
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel
import dev.gaborbiro.dailymacros.ui.components.CompactMacroNutrientsGrid
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

    ScrollableContentDialog(
        onDismissRequested = onDismissRequested,
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
                analysis = ui.analysis,
                showProgressIndicator = ui.showProgressIndicator,
                description = ui.description,
                titleErrorMessage = ui.titleValidationError,
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
                showQuickPickStar = viewDialog != null && viewDialog.allowEdit,
                quickPickStarred = viewDialog?.quickPickStarred == true,
                onQuickPickStarToggled = onQuickPickStarToggled,
                onBeginViewEdit = onRecordDetailsEditStarted,
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
                                saveAndAddLabel = if (dirty) {
                                    stringResource(R.string.meal_details_action_add_new_template)
                                } else {
                                    stringResource(R.string.meal_details_action_add_new)
                                },
                                onUpdate = onSaveDetailsTapped,
                                onSaveAndAdd = onSaveAndAddDetailsTapped,
                                onCancel = onRecordDetailsEditCancelled,
                            )
                        }

                        else -> {
                            RecordDetailsViewBrowseButtons(
                                onLogMealAgain = onSaveAndAddDetailsTapped,
                                onDismissRequested = onDismissRequested,
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
    analysis: RecognisedFood?,
    showProgressIndicator: Boolean,
    description: TextFieldValue,
    titleErrorMessage: String?,
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
    onBeginViewEdit: () -> Unit = { },
) {
    when (dialogHandle) {
        is DialogHandle.RecordDetailsDialog.Edit ->
            RecordDetailsCreateBody(
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
                onAddImageViaCameraTapped = onAddImageViaCameraTapped,
                onAddImageViaPickerTapped = onAddImageViaPickerTapped,
                onImagesInfoButtonTapped = onImagesInfoButtonTapped,
                onRunAIButtonTapped = onRunAIButtonTapped,
            )

        is DialogHandle.RecordDetailsDialog.View ->
            RecordDetailsViewBody(
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

@Composable
private fun ColumnScope.RecordDetailsCreateBody(
    onTitleChanged: (TextFieldValue) -> Unit,
    onDescriptionChanged: (TextFieldValue) -> Unit,
    showKeyboardOnOpen: Boolean,
    images: List<String>,
    title: TextFieldValue,
    showRunAIButton: Boolean,
    titleHint: String,
    showProgressIndicator: Boolean,
    description: TextFieldValue,
    titleErrorMessage: String?,
    onImageTapped: (String) -> Unit,
    onImageDeleteTapped: (String) -> Unit,
    onAddImageViaCameraTapped: () -> Unit,
    onAddImageViaPickerTapped: () -> Unit,
    onImagesInfoButtonTapped: () -> Unit,
    onRunAIButtonTapped: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    if (showKeyboardOnOpen) {
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
        showAddPhotoButtons = true,
        showImageDeleteButton = true,
        showInfoButton = true,
        images = images,
        onImageTapped = onImageTapped,
        onImageDeleteTapped = onImageDeleteTapped,
        onAddImageViaCameraTapped = onAddImageViaCameraTapped,
        onAddImageViaPickerTapped = onAddImageViaPickerTapped,
        onInfoButtonTapped = onImagesInfoButtonTapped,
    )

    TextField(
        modifier = Modifier
            .padding(horizontal = PaddingDefault)
            .fillMaxWidth()
            .wrapContentHeight()
            .focusRequester(focusRequester),
        isError = titleErrorMessage.isNullOrBlank().not(),
        textStyle = MaterialTheme.typography.bodyMedium,
        trailingIcon = {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    showProgressIndicator -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(25.dp),
                        )
                    }
                    showRunAIButton -> {
                        Button(
                            modifier = Modifier
                                .size(48.dp)
                                .padding(PaddingHalf),
                            contentPadding = PaddingValues(),
                            onClick = onRunAIButtonTapped,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chatgpt),
                                contentDescription = "Run image recognition",
                            )
                        }
                    }
                }
            }
        },
        placeholder = {
            Text(
                text = titleHint,
                style = MaterialTheme.typography.labelLarge,
                fontStyle = FontStyle.Italic,
                color = Color.Gray,
            )
        },
        value = title,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next,
        ),
        onValueChange = { onTitleChanged(it) },
    )
    if (titleErrorMessage.isNullOrBlank().not()) {
        Text(
            text = titleErrorMessage.orEmpty(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .height(16.dp)
                .padding(horizontal = PaddingDefault),
        )
    }

    Spacer(modifier = Modifier.height(PaddingDefault))

    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaddingDefault)
            .height(96.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = {
            Text(
                text = stringResource(R.string.meal_details_description_placeholder),
                style = MaterialTheme.typography.labelLarge,
                fontStyle = FontStyle.Italic,
                color = Color.Gray,
            )
        },
        value = description,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences,
        ),
        onValueChange = { onDescriptionChanged(it) },
    )
}

@Composable
private fun ColumnScope.RecordDetailsViewBody(
    view: DialogHandle.RecordDetailsDialog.View,
    showCloseOnly: Boolean,
    showPhotoManagement: Boolean,
    onTitleChanged: (TextFieldValue) -> Unit,
    onDescriptionChanged: (TextFieldValue) -> Unit,
    title: TextFieldValue,
    titleHint: String,
    titleErrorMessage: String?,
    description: TextFieldValue,
    nutrientBreakdown: NutrientBreakdownUiModel?,
    images: List<String>,
    onImageTapped: (String) -> Unit,
    onImageDeleteTapped: (String) -> Unit,
    onAddImageViaCameraTapped: () -> Unit,
    onAddImageViaPickerTapped: () -> Unit,
    onImagesInfoButtonTapped: () -> Unit,
    variantPickerOptions: List<MealVariantPickerOption>?,
    selectedVariantTemplateId: Long,
    onVariantTemplatePicked: (Long) -> Unit,
    showQuickPickStar: Boolean,
    quickPickStarred: Boolean,
    onQuickPickStarToggled: () -> Unit,
    onBeginViewEdit: () -> Unit,
) {
    val browseMode = !view.isEditing
    val browseInteractive = browseMode && view.allowEdit && !showCloseOnly
    val browseReadOnly = browseMode && (showCloseOnly || !view.allowEdit)
    var macrosExpanded by remember(view.recordId, view.templateDbId) { mutableStateOf(false) }

    ImageStrip(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PaddingDefault)
            .padding(bottom = PaddingDefault),
        showAddPhotoButtons = showPhotoManagement,
        showImageDeleteButton = showPhotoManagement,
        showInfoButton = showPhotoManagement,
        images = images,
        onImageTapped = onImageTapped,
        onImageDeleteTapped = onImageDeleteTapped,
        onAddImageViaCameraTapped = onAddImageViaCameraTapped,
        onAddImageViaPickerTapped = onAddImageViaPickerTapped,
        onInfoButtonTapped = onImagesInfoButtonTapped,
    )

    if (browseMode && !variantPickerOptions.isNullOrEmpty()) {
        VariantTemplateDropdown(
            options = variantPickerOptions,
            selectedTemplateId = selectedVariantTemplateId,
            titleHint = titleHint,
            titleErrorMessage = titleErrorMessage,
            onTemplatePicked = onVariantTemplatePicked,
        )
    }

    if (view.isEditing) {
        TextField(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .fillMaxWidth()
                .wrapContentHeight(),
            isError = titleErrorMessage.isNullOrBlank().not(),
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = {
                Text(
                    text = titleHint,
                    style = MaterialTheme.typography.labelLarge,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray,
                )
            },
            value = title,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            onValueChange = { onTitleChanged(it) },
        )
        if (titleErrorMessage.isNullOrBlank().not()) {
            Text(
                text = titleErrorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .height(16.dp)
                    .padding(horizontal = PaddingDefault),
            )
        }
        Spacer(modifier = Modifier.height(PaddingDefault))
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingDefault)
                .height(96.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = {
                Text(
                    text = stringResource(R.string.meal_details_description_placeholder),
                    style = MaterialTheme.typography.labelLarge,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray,
                )
            },
            value = description,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
            ),
            onValueChange = { onDescriptionChanged(it) },
        )
    } else if (browseInteractive || browseReadOnly) {
        Row(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title.text,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (showQuickPickStar) {
                IconButton(onClick = onQuickPickStarToggled) {
                    Icon(
                        imageVector = if (quickPickStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (quickPickStarred) {
                            stringResource(R.string.meal_details_quick_pick_unstar_cd)
                        } else {
                            stringResource(R.string.meal_details_quick_pick_star_cd)
                        },
                        tint = if (quickPickStarred) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            if (browseInteractive) {
                IconButton(onClick = onBeginViewEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.meal_details_begin_edit_cd),
                    )
                }
            }
        }

        if (description.text.isNotBlank()) {
            Text(
                modifier = Modifier
                    .padding(horizontal = PaddingDefault)
                    .padding(top = PaddingDefault)
                    .fillMaxWidth(),
                text = description.text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (nutrientBreakdown != null) {
            Spacer(modifier = Modifier.height(PaddingDefault))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingDefault),
            ) {
                CompactMacroNutrientsGrid(
                    modifier = Modifier.fillMaxWidth(),
                    nutrients = view.compactNutrients,
                )
                Spacer(modifier = Modifier.height(PaddingHalf))
                if (macrosExpanded) {
                    NutrientsIndentedList(nutrientBreakdown = nutrientBreakdown)
                    Spacer(modifier = Modifier.height(PaddingHalf))
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { macrosExpanded = !macrosExpanded },
                    contentPadding = PaddingValues(vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (macrosExpanded) {
                                stringResource(R.string.meal_details_collapse)
                            } else {
                                stringResource(R.string.meal_details_expand)
                            },
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Icon(
                            imageVector = if (macrosExpanded) {
                                Icons.Filled.KeyboardArrowUp
                            } else {
                                Icons.Filled.KeyboardArrowDown
                            },
                            contentDescription = null,
                        )
                    }
                }
            }
        } else if (view.compactNutrients.hasAnyVisibleMacro()) {
            Spacer(modifier = Modifier.height(PaddingDefault))
            CompactMacroNutrientsGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingDefault),
                nutrients = view.compactNutrients,
            )
        }
    }
}

private fun NutrientsUiModel.hasAnyVisibleMacro(): Boolean =
    sequenceOf(calories, protein, fat, carbs, salt, fibre).any { !it.isNullOrBlank() }

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
    val menuScrollState = rememberScrollState()
    val selectedOption = options.find { it.templateId == selectedTemplateId } ?: options.firstOrNull()
    val summaryForField = selectedOption?.let { "${it.title}\n${it.lastUsedDateLabel}" } ?: ""
    val pickVariantLabel = stringResource(R.string.meal_details_pick_variant_cd)
    val selectedRowLabel = stringResource(R.string.meal_details_variant_row_selected_cd)
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
            scrollState = menuScrollState,
        ) {
            options.forEach { option ->
                val isSelected = option.templateId == selectedTemplateId
                DropdownMenuItem(
                    modifier = Modifier.semantics {
                        selected = isSelected
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = selectedRowLabel,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    },
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
