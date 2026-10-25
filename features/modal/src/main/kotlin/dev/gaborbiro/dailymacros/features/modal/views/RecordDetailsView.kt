package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.features.modal.R
import dev.gaborbiro.dailymacros.features.common.R as CommonR
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.design.PaddingQuarter
import dev.gaborbiro.dailymacros.features.common.utils.diaryDayStartTime
import dev.gaborbiro.dailymacros.features.common.utils.logicalDiaryDate
import dev.gaborbiro.dailymacros.features.common.utils.logicalDiaryToday
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.MealVariantPickerOption
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel
import dev.gaborbiro.dailymacros.features.modal.model.RecordDetailsPristineSnapshot
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel
import dev.gaborbiro.dailymacros.features.shared.views.CompactMacroNutrientsGrid
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val recordTimestampFormatter = DateTimeFormatter.ofPattern("dd MMM, H:mm")
private val recordTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

/**
 * A logged meal, read top to bottom: photos, title with when it was eaten, description, the macros and what
 * the AI inferred (with a link to correct it). Kept deliberately flat: no cards, borders or dividers, and the
 * Quick Pick star lives in the dialog's header (see RecordDetailsDialog).
 *
 * Browsing and editing share this one layout: switching to [DialogHandle.RecordDetailsDialog.View.isEditing]
 * turns the title and description into fields in place, slides in the photo controls and folds away the
 * parts that only make sense while browsing.
 */
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
    imageFilenames: List<String>,
    macrosExpanded: Boolean = false,
    aiInferenceExpanded: Boolean = false,
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
    onEditTimestampTapped: () -> Unit = {},
    onCorrectionChanged: (TextFieldValue) -> Unit = {},
    onReanalyzeTapped: () -> Unit = {},
    onTitleSpoken: (String) -> Unit = {},
    onDescriptionSpoken: (String) -> Unit = {},
    onCorrectionSpoken: (String) -> Unit = {},
    onVoiceInputFailed: (Throwable) -> Unit = {},
) {
    val editing = view.isEditing
    val browseMode = !editing
    val browseInteractive = browseMode && view.allowEdit && !showCloseOnly
    val showImageControls = showPhotoManagement || (editing && view.allowEdit && !showCloseOnly)
    var macrosExpanded by remember(view.recordId, view.templateDbId) { mutableStateOf(macrosExpanded) }
    var aiInferenceExpanded by remember(view.recordId, view.templateDbId) { mutableStateOf(aiInferenceExpanded) }
    // Session-scoped: survives View state replacements (e.g. variant switch) until this dialog leaves composition.
    var variantPickerRevealed by remember { mutableStateOf(false) }

    val variantRowShown = browseMode && !variantPickerOptions.isNullOrEmpty()
    AnimatedVisibility(visible = variantRowShown, enter = editModeEnter, exit = editModeExit) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                variantPickerOptions?.let { options ->
                    VariantTemplateDropdown(
                        options = options,
                        selectedTemplateId = selectedVariantTemplateId,
                        titleHint = titleHint,
                        titleErrorMessage = titleErrorMessage,
                        onTemplatePicked = onVariantTemplatePicked,
                    )
                }
            }
        }
    }

    val hasImages = imageFilenames.isNotEmpty()
    // While browsing, the header's star and overflow menu (RecordDetailsDialog's topEndAction) float over the
    // strip's end, so keep photos from scrolling under them: 12dp outer padding + 2 x 48dp IconButtons.
    val imageStripEndPadding by animateDpAsState(
        targetValue = if (browseInteractive) 104.dp else 0.dp,
        animationSpec = editModeSpec(),
        label = "mealDetailsImageStripEndPadding",
    )
    AnimatedVisibility(visible = hasImages || showImageControls, enter = editModeEnter, exit = editModeExit) {
        ImageStrip(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = imageStripEndPadding)
                .padding(top = 12.dp)
                .padding(bottom = 12.dp),
            showAddPhotoButtons = showImageControls,
            showImageDeleteButton = showImageControls,
            showImageReorderButtons = showImageControls,
            showInfoButton = showImageControls,
            imageFilenames = imageFilenames,
            onImageTapped = onImageTapped,
            onImageDeleteTapped = onImageDeleteTapped,
            onImageMoveLeftTapped = onImageMoveLeftTapped,
            onImageMoveRightTapped = onImageMoveRightTapped,
            onAddImageViaCameraTapped = onAddImageViaCameraTapped,
            onAddImageViaPickerTapped = onAddImageViaPickerTapped,
            onInfoButtonTapped = onImagesInfoButtonTapped,
        )
    }

    // Without a photo strip, the title is the first thing in the dialog, right under the fixed overflow
    // menu (RecordDetailsDialog's topEndAction). Clear its footprint (12dp top padding + 48dp default
    // IconButton) so a long title doesn't run under it.
    val titleTopPadding by animateDpAsState(
        targetValue = when {
            editing -> 0.dp
            browseInteractive && !hasImages && !variantRowShown -> 60.dp
            else -> 12.dp
        },
        animationSpec = editModeSpec(),
        label = "mealDetailsTitleTopPadding",
    )
    InPlaceTextField(
        modifier = Modifier
            .padding(horizontal = PaddingDefault)
            .padding(top = titleTopPadding)
            .fillMaxWidth(),
        value = title,
        onValueChange = onTitleChanged,
        editing = editing,
        textStyle = MaterialTheme.typography.titleMedium,
        browseTextColor = MaterialTheme.colorScheme.onSurface,
        placeholder = titleHint,
        isError = titleErrorMessage.isNullOrBlank().not(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next,
        ),
        trailingIcon = {
            VoiceInputButton(
                prompt = stringResource(R.string.modal_content_voice_input_prompt),
                contentDescription = stringResource(R.string.modal_content_voice_input_cd),
                onTextRecognised = onTitleSpoken,
                onLaunchFailed = onVoiceInputFailed,
            )
        },
    )
    AnimatedVisibility(visible = browseMode, enter = editModeEnter, exit = editModeExit) {
        TimestampLine(
            view = view,
            editable = browseInteractive,
            onEditTimestampTapped = onEditTimestampTapped,
        )
    }
    AnimatedVisibility(visible = editing && titleErrorMessage.isNullOrBlank().not()) {
        Text(
            modifier = Modifier
                .height(16.dp)
                .padding(horizontal = PaddingDefault),
            text = titleErrorMessage.orEmpty(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
        )
    }

    AnimatedVisibility(
        visible = editing || description.text.isNotBlank(),
        enter = editModeEnter,
        exit = editModeExit,
    ) {
        InPlaceTextField(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(top = 12.dp)
                .fillMaxWidth(),
            value = description,
            onValueChange = onDescriptionChanged,
            editing = editing,
            textStyle = MaterialTheme.typography.bodyLarge,
            browseTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            placeholder = stringResource(R.string.meal_details_description_placeholder),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
            ),
            editingMinHeight = 120.dp,
            trailingIcon = {
                VoiceInputButton(
                    prompt = stringResource(R.string.modal_content_voice_input_description_prompt),
                    contentDescription = stringResource(R.string.modal_content_voice_input_description_cd),
                    onTextRecognised = onDescriptionSpoken,
                    onLaunchFailed = onVoiceInputFailed,
                )
            },
        )
    }

    AnimatedVisibility(visible = browseMode, enter = editModeEnter, exit = editModeExit) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val correctionInput: (@Composable () -> Unit)? = if (view.allowCorrection && browseInteractive) {
                {
                    MealCorrectionInput(
                        correction = view.correction,
                        canReanalyze = view.canReanalyze,
                        onCorrectionChanged = onCorrectionChanged,
                        onCorrectionSpoken = onCorrectionSpoken,
                        onVoiceInputFailed = onVoiceInputFailed,
                        onReanalyzeTapped = onReanalyzeTapped,
                    )
                }
            } else {
                null
            }

            if (view.showLoadingIndicator) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = PaddingDefault)
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(CommonR.drawable.ic_ai_borg),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = PaddingHalf)
                            .size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.record_details_analyzing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }

            if (!view.showLoadingIndicator && nutrientBreakdown?.hasDisplayableContent == true) {
                Spacer(modifier = Modifier.height(20.dp))
                MacrosCard(
                    nutrientBreakdown = nutrientBreakdown,
                    compactNutrients = view.compactNutrients,
                    expanded = macrosExpanded,
                    onExpandedChange = { macrosExpanded = it },
                )

                if (nutrientBreakdown.hasAiInference) {
                    Spacer(modifier = Modifier.height(PaddingDefault))
                    AiInferenceSection(
                        nutrientBreakdown = nutrientBreakdown,
                        expanded = aiInferenceExpanded,
                        onExpandedChange = { aiInferenceExpanded = it },
                        correctionInput = correctionInput,
                    )
                } else {
                    correctionInput?.invoke()
                }
            } else if (!view.showLoadingIndicator && view.compactNutrients.hasAnyVisibleMacro()) {
                Spacer(modifier = Modifier.height(20.dp))
                CompactMacroNutrientsGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaddingDefault),
                    nutrients = view.compactNutrients,
                )
                correctionInput?.invoke()
            }
        }
    }
}

/** The macros at a glance; tapping them (or "More details") unfolds the full breakdown. */
@Composable
private fun MacrosCard(
    nutrientBreakdown: NutrientBreakdownUiModel,
    compactNutrients: NutrientsUiModel,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val macroFadeSpec = tween<Float>(durationMillis = 400)
    // Tapping the chips expands, but only the "Details" pill shows the press (see ExpandPill).
    val toggleInteractions = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .let {
                if (expanded) {
                    it
                } else {
                    it.clickable(interactionSource = toggleInteractions, indication = null) { onExpandedChange(true) }
                }
            }
            .animateContentSize(animationSpec = tween(durationMillis = 280)),
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                fadeIn(macroFadeSpec) togetherWith fadeOut(macroFadeSpec)
            },
            label = "mealDetailsMacrosExpand",
        ) { isExpanded ->
            if (isExpanded) {
                NutrientsIndentedList(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaddingDefault, vertical = PaddingHalf),
                    nutrientBreakdown = nutrientBreakdown
                )
            } else {
                CompactMacroNutrientsGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaddingDefault),
                    nutrients = compactNutrients,
                )
            }
        }

        ExpandToggleRow(
            expanded = expanded,
            onCollapseTapped = { onExpandedChange(false) },
            interactionSource = toggleInteractions,
        )
    }
}

/** When the meal was eaten, as a quiet line under the title. Tapping it (on an editable meal) opens the picker. */
@Composable
private fun TimestampLine(
    view: DialogHandle.RecordDetailsDialog.View,
    editable: Boolean,
    onEditTimestampTapped: () -> Unit,
) {
    val dayStart = diaryDayStartTime(view.diaryDayStartHour)
    val recordDiaryDate = view.timestamp.logicalDiaryDate(dayStart)
    val today = logicalDiaryToday(view.timestamp.zone, dayStart)
    val relativeDayLabel = when (recordDiaryDate) {
        today -> stringResource(R.string.edit_timestamp_day_today)
        today.minusDays(1) -> stringResource(R.string.edit_timestamp_day_yesterday)
        else -> null
    }
    // "Today" already says which day, so the date only shows for older meals.
    val formattedTimestamp = relativeDayLabel
        ?.let { "$it, ${view.timestamp.format(recordTimeFormatter)}" }
        ?: view.timestamp.format(recordTimestampFormatter)
    val editLabel = stringResource(R.string.meal_details_edit_timestamp_cd)
    Row(
        modifier = Modifier
            .padding(horizontal = PaddingHalf)
            .then(
                if (editable) {
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClickLabel = editLabel) { onEditTimestampTapped() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = PaddingHalf, vertical = PaddingQuarter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formattedTimestamp,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (editable) {
            // Just enough to say "this can change"; the text stays the main thing.
            Icon(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(14.dp),
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .6f),
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
            modifier = Modifier.exposedDropdownSize(),
            expanded = expanded,
            onDismissRequest = { expanded = false },
            scrollState = menuScrollState,
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = option.templateId == selectedTemplateId
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 48.dp, end = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
                VariantTemplateMenuItem(
                    option = option,
                    isSelected = isSelected,
                    selectedRowLabel = selectedRowLabel,
                    onClick = {
                        expanded = false
                        onTemplatePicked(option.templateId)
                    },
                )
            }
        }
    }
}

@Composable
private fun VariantTemplateMenuItem(
    option: MealVariantPickerOption,
    isSelected: Boolean,
    selectedRowLabel: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        modifier = Modifier
            .then(
                if (isSelected) {
                    Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                } else {
                    Modifier
                }
            )
            .semantics { selected = isSelected },
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
            Column(Modifier.padding(vertical = 10.dp)) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = option.lastUsedDateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (option.componentsSubtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = option.componentsSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        onClick = onClick,
    )
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
        titleHint = "Title (optional)",
        description = TextFieldValue("I ate an apple"),
        imageFilenames = listOf("1", "2"),
        allowEdit = true,
        nutrientBreakdown = previewNutrientBreakdown(),
        compactNutrients = previewCompactNutrients(),
        pristineSnapshot = RecordDetailsPristineSnapshot(
            templateDbId = 1L,
            title = "Apple",
            description = "I ate an apple",
            imageFilenames = listOf("1", "2"),
        ),
        linkedRecordCountForTemplate = 3,
        timestamp = ZonedDateTime.now(),
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
            imageFilenames = view.imageFilenames,
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
        titleHint = "Title (optional)",
        description = TextFieldValue("I ate an apple"),
        imageFilenames = listOf("1", "2"),
        allowEdit = true,
        nutrientBreakdown = previewNutrientBreakdown(),
        compactNutrients = previewCompactNutrients(),
        pristineSnapshot = RecordDetailsPristineSnapshot(
            templateDbId = 1L,
            title = "Apple",
            description = "I ate an apple",
            imageFilenames = listOf("1", "2"),
        ),
        linkedRecordCountForTemplate = 3,
        timestamp = ZonedDateTime.now(),
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
            imageFilenames = view.imageFilenames,
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
            macrosExpanded = true,
            aiInferenceExpanded = true,
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
        titleHint = "Title (optional)",
        description = TextFieldValue("I ate an apple"),
        imageFilenames = listOf("1", "2"),
        allowEdit = true,
        isEditing = true,
        nutrientBreakdown = previewNutrientBreakdown(),
        compactNutrients = previewCompactNutrients(),
        pristineSnapshot = RecordDetailsPristineSnapshot(
            templateDbId = 1L,
            title = "Apple",
            description = "I ate an apple",
            imageFilenames = listOf("1", "2"),
        ),
        timestamp = ZonedDateTime.now(),
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
            imageFilenames = view.imageFilenames,
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
        titleHint = "Title (optional)",
        description = TextFieldValue(),
        imageFilenames = listOf("1"),
        allowEdit = true,
        nutrientBreakdown = null,
        compactNutrients = previewCompactNutrients(),
        variantPickerOptions = listOf(
            MealVariantPickerOption(
                templateId = 1L,
                title = "Chicken curry",
                lastUsedDateLabel = "Wed, 27 May",
                isCurrentVariant = true,
                componentsSubtitle = "chicken thigh, rice, coconut milk, curry paste",
            ),
            MealVariantPickerOption(
                templateId = 2L,
                title = "Chicken curry (large)",
                lastUsedDateLabel = "Tue, 26 May",
                isCurrentVariant = false,
                componentsSubtitle = "chicken breast, basmati rice, yoghurt, naan",
            ),
        ),
        pristineSnapshot = RecordDetailsPristineSnapshot(
            templateDbId = 1L,
            title = "Chicken curry",
            description = "",
            imageFilenames = listOf("1"),
        ),
        timestamp = ZonedDateTime.now(),
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
            imageFilenames = view.imageFilenames,
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
        )
    }
}

private fun previewNutrientBreakdown() = NutrientBreakdownUiModel(
    nutrients = previewNutrientRows(),
    notes = "Notes: This is a note",
    components = listOf("- 1 apple", "- ~1 tbsp peanut butter (likely)"),
    componentsSummary = "1 apple · ~1 tbsp peanut butter (likely)",
)

private fun previewCompactNutrients() = NutrientsUiModel(
    calories = "2100kcal",
    protein = "Protein 150g",
    fat = "Fat 100g",
    carbs = "Carbs 100g",
    salt = "Salt 5g",
    fibre = "Fibre 4.5g",
)
