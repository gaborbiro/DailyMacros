package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.MealVariantPickerOption
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel
import dev.gaborbiro.dailymacros.features.modal.model.RecordDetailsPristineSnapshot
import dev.gaborbiro.dailymacros.features.modal.model.hasDisplayableContent
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel
import dev.gaborbiro.dailymacros.ui.components.CompactMacroNutrientsGrid

@Composable
fun ColumnScope.RecordDetailsView(
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
    macrosExpanded: Boolean = false,
    onImageTapped: (String) -> Unit,
    onImageDeleteTapped: (String) -> Unit,
    onImageMoveLeftTapped: (String) -> Unit,
    onImageMoveRightTapped: (String) -> Unit,
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
    val showImageControls = showPhotoManagement || (view.isEditing && view.allowEdit && !showCloseOnly)
    var macrosExpanded by remember(view.recordId, view.templateDbId) { mutableStateOf(macrosExpanded) }
    // Session-scoped: survives View state replacements (e.g. variant switch) until this dialog leaves composition.
    var variantPickerRevealed by remember { mutableStateOf(false) }

    if (browseMode && !variantPickerOptions.isNullOrEmpty()) {
        if (!variantPickerRevealed) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingDefault)
                    .padding(top = PaddingDefault)
                    .padding(vertical = 8.dp)
                    .clickable { variantPickerRevealed = true },
                text = stringResource(R.string.meal_details_variant_different_link),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                ),
                textAlign = TextAlign.Start,
            )
        } else {
            VariantTemplateDropdown(
                options = variantPickerOptions,
                selectedTemplateId = selectedVariantTemplateId,
                titleHint = titleHint,
                titleErrorMessage = titleErrorMessage,
                onTemplatePicked = onVariantTemplatePicked,
            )
        }
    }

    ImageStrip(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .padding(bottom = 12.dp),
        showAddPhotoButtons = showImageControls,
        showImageDeleteButton = showImageControls,
        showImageReorderButtons = showImageControls,
        showInfoButton = showImageControls,
        images = images,
        onImageTapped = onImageTapped,
        onImageDeleteTapped = onImageDeleteTapped,
        onImageMoveLeftTapped = onImageMoveLeftTapped,
        onImageMoveRightTapped = onImageMoveRightTapped,
        onAddImageViaCameraTapped = onAddImageViaCameraTapped,
        onAddImageViaPickerTapped = onAddImageViaPickerTapped,
        onInfoButtonTapped = onImagesInfoButtonTapped,
    )

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
                modifier = Modifier
                    .height(16.dp)
                    .padding(horizontal = PaddingDefault),
                text = titleErrorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
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
                .heightIn(min = 120.dp),
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
                .padding(top = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title.text,
                style = MaterialTheme.typography.titleMedium,
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
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
                text = description.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (nutrientBreakdown?.hasDisplayableContent() == true) {
            Spacer(
                modifier = Modifier
                    .height(20.dp)
            )

            val macroFadeSpec = tween<Float>(durationMillis = 400)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingHalf)
                    .let {
                        if (macrosExpanded) {
                            it
                        } else {
                            it.clickable { macrosExpanded = true }
                        }
                    },
                color = Color.Transparent,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = .45f)),
            ) {
                Column(
                    modifier = Modifier
                        .animateContentSize(animationSpec = tween(durationMillis = 280)),
                ) {
                    AnimatedContent(
                        targetState = macrosExpanded,
                        transitionSpec = {
                            fadeIn(macroFadeSpec) togetherWith fadeOut(macroFadeSpec)
                        },
                        label = "mealDetailsMacrosExpand",
                    ) { expanded ->
                        if (expanded) {
                            NutrientsIndentedList(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(PaddingHalf),
                                nutrientBreakdown = nutrientBreakdown
                            )
                        } else {
                            CompactMacroNutrientsGrid(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(PaddingHalf),
                                nutrients = view.compactNutrients,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .height(28.dp)
                            .fillMaxWidth()
                            .let {
                                if (macrosExpanded) {
                                    it.clickable { macrosExpanded = false }
                                } else {
                                    it
                                }
                            },
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

                        Spacer(
                            modifier = Modifier
                                .size(6.dp)
                        )

                        Icon(
                            imageVector = if (macrosExpanded) {
                                Icons.Filled.KeyboardArrowUp
                            } else {
                                Icons.Filled.KeyboardArrowDown
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        } else if (view.compactNutrients.hasAnyVisibleMacro()) {
            Spacer(modifier = Modifier.height(20.dp))
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
            .padding(top = PaddingDefault, bottom = 8.dp)
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
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = option.lastUsedDateLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (option.componentsSubtitle.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = option.componentsSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
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
private fun RecordDetailsViewPreviewBrowse() {
    val view = DialogHandle.RecordDetailsDialog.View(
        recordId = 1L,
        templateDbId = 1L,
        variabilityAnchorTemplateDbId = 1L,
        title = TextFieldValue("Apple"),
        titleHint = "Give your meal a title",
        description = TextFieldValue("I ate an apple"),
        images = listOf("1", "2"),
        allowEdit = true,
        nutrientBreakdown = previewNutrientBreakdown(),
        compactNutrients = previewCompactNutrients(),
        pristineSnapshot = RecordDetailsPristineSnapshot(
            templateDbId = 1L,
            title = "Apple",
            description = "I ate an apple",
            images = listOf("1", "2"),
        ),
        linkedRecordCountForTemplate = 3,
    )
    ViewPreviewContext {
        RecordDetailsView(
            view = view,
            showCloseOnly = false,
            showPhotoManagement = false,
            onTitleChanged = {},
            onDescriptionChanged = {},
            title = view.title,
            titleHint = view.titleHint,
            titleErrorMessage = null,
            description = view.description,
            nutrientBreakdown = view.nutrientBreakdown,
            images = view.images,
            onImageTapped = {},
            onImageDeleteTapped = {},
            onImageMoveLeftTapped = {},
            onImageMoveRightTapped = {},
            onAddImageViaCameraTapped = {},
            onAddImageViaPickerTapped = {},
            onImagesInfoButtonTapped = {},
            variantPickerOptions = null,
            selectedVariantTemplateId = view.templateDbId,
            onVariantTemplatePicked = {},
            showQuickPickStar = true,
            quickPickStarred = false,
            onQuickPickStarToggled = {},
            onBeginViewEdit = {},
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun RecordDetailsViewPreviewBrowseExpanded() {
    val view = DialogHandle.RecordDetailsDialog.View(
        recordId = 1L,
        templateDbId = 1L,
        variabilityAnchorTemplateDbId = 1L,
        title = TextFieldValue("Apple"),
        titleHint = "Give your meal a title",
        description = TextFieldValue("I ate an apple"),
        images = listOf("1", "2"),
        allowEdit = true,
        nutrientBreakdown = previewNutrientBreakdown(),
        compactNutrients = previewCompactNutrients(),
        pristineSnapshot = RecordDetailsPristineSnapshot(
            templateDbId = 1L,
            title = "Apple",
            description = "I ate an apple",
            images = listOf("1", "2"),
        ),
        linkedRecordCountForTemplate = 3,
    )
    ViewPreviewContext {
        RecordDetailsView(
            view = view,
            showCloseOnly = false,
            showPhotoManagement = false,
            onTitleChanged = {},
            onDescriptionChanged = {},
            title = view.title,
            titleHint = view.titleHint,
            titleErrorMessage = null,
            description = view.description,
            nutrientBreakdown = view.nutrientBreakdown,
            images = view.images,
            onImageTapped = {},
            onImageDeleteTapped = {},
            onImageMoveLeftTapped = {},
            onImageMoveRightTapped = {},
            onAddImageViaCameraTapped = {},
            onAddImageViaPickerTapped = {},
            onImagesInfoButtonTapped = {},
            variantPickerOptions = null,
            selectedVariantTemplateId = view.templateDbId,
            onVariantTemplatePicked = {},
            showQuickPickStar = true,
            quickPickStarred = false,
            onQuickPickStarToggled = {},
            onBeginViewEdit = {},
            macrosExpanded = true,
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun RecordDetailsViewPreviewEditing() {
    val view = DialogHandle.RecordDetailsDialog.View(
        recordId = 1L,
        templateDbId = 1L,
        variabilityAnchorTemplateDbId = 1L,
        title = TextFieldValue("Apple"),
        titleHint = "Give your meal a title",
        description = TextFieldValue("I ate an apple"),
        images = listOf("1", "2"),
        allowEdit = true,
        isEditing = true,
        nutrientBreakdown = previewNutrientBreakdown(),
        compactNutrients = previewCompactNutrients(),
        pristineSnapshot = RecordDetailsPristineSnapshot(
            templateDbId = 1L,
            title = "Apple",
            description = "I ate an apple",
            images = listOf("1", "2"),
        ),
    )
    ViewPreviewContext {
        RecordDetailsView(
            view = view,
            showCloseOnly = false,
            showPhotoManagement = true,
            onTitleChanged = {},
            onDescriptionChanged = {},
            title = view.title,
            titleHint = view.titleHint,
            titleErrorMessage = null,
            description = view.description,
            nutrientBreakdown = view.nutrientBreakdown,
            images = view.images,
            onImageTapped = {},
            onImageDeleteTapped = {},
            onImageMoveLeftTapped = {},
            onImageMoveRightTapped = {},
            onAddImageViaCameraTapped = {},
            onAddImageViaPickerTapped = {},
            onImagesInfoButtonTapped = {},
            variantPickerOptions = null,
            selectedVariantTemplateId = view.templateDbId,
            onVariantTemplatePicked = {},
            showQuickPickStar = false,
            quickPickStarred = false,
            onQuickPickStarToggled = {},
            onBeginViewEdit = {},
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun RecordDetailsViewPreviewVariantPicker() {
    val view = DialogHandle.RecordDetailsDialog.View(
        recordId = 1L,
        templateDbId = 1L,
        variabilityAnchorTemplateDbId = 1L,
        title = TextFieldValue("Chicken curry"),
        titleHint = "Give your meal a title",
        description = TextFieldValue(),
        images = listOf("1"),
        allowEdit = true,
        nutrientBreakdown = null,
        compactNutrients = previewCompactNutrients(),
        variantPickerOptions = listOf(
            MealVariantPickerOption(
                templateId = 1L,
                title = "Chicken curry",
                lastUsedDateLabel = "Wed, 27 May",
                isCurrentVariant = true,
            ),
            MealVariantPickerOption(
                templateId = 2L,
                title = "Chicken curry (large)",
                lastUsedDateLabel = "Tue, 26 May",
                isCurrentVariant = false,
            ),
        ),
        pristineSnapshot = RecordDetailsPristineSnapshot(
            templateDbId = 1L,
            title = "Chicken curry",
            description = "",
            images = listOf("1"),
        ),
    )
    ViewPreviewContext {
        RecordDetailsView(
            view = view,
            showCloseOnly = false,
            showPhotoManagement = false,
            onTitleChanged = {},
            onDescriptionChanged = {},
            title = view.title,
            titleHint = view.titleHint,
            titleErrorMessage = null,
            description = view.description,
            nutrientBreakdown = view.nutrientBreakdown,
            images = view.images,
            onImageTapped = {},
            onImageDeleteTapped = {},
            onImageMoveLeftTapped = {},
            onImageMoveRightTapped = {},
            onAddImageViaCameraTapped = {},
            onAddImageViaPickerTapped = {},
            onImagesInfoButtonTapped = {},
            variantPickerOptions = view.variantPickerOptions,
            selectedVariantTemplateId = view.templateDbId,
            onVariantTemplatePicked = {},
            showQuickPickStar = true,
            quickPickStarred = true,
            onQuickPickStarToggled = {},
            onBeginViewEdit = {},
        )
    }
}

private fun previewNutrientBreakdown() = NutrientBreakdownUiModel(
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
)

private fun previewCompactNutrients() = NutrientsUiModel(
    calories = "2100kcal",
    protein = "Protein 150g",
    fat = "Fat 100g",
    carbs = "Carbs 100g",
    salt = "Salt 5g",
    fibre = "Fibre 4.5g",
)
