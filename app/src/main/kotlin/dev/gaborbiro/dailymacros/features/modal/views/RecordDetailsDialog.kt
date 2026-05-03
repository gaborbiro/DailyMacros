package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel
import dev.gaborbiro.dailymacros.features.modal.model.RecognisedFood
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityPreviewContent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilitySlotPreview
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityVariantPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow


@Composable
internal fun RecordDetailsDialog(
    dialogHandle: DialogHandle.RecordDetailsDialog,
    errorMessages: Flow<String>,
    onTitleChanged: (TextFieldValue) -> Unit,
    onDescriptionChanged: (TextFieldValue) -> Unit,
    onSubmitButtonTapped: () -> Unit,
    onImageTapped: (String) -> Unit,
    onImageDeleteTapped: (String) -> Unit,
    onAddImageViaCameraTapped: () -> Unit,
    onAddImageViaPickerTapped: () -> Unit,
    onDismissRequested: () -> Unit,
    onImagesInfoButtonTapped: () -> Unit,
    onRunAIButtonTapped: () -> Unit,
    onVariabilityDifferentMealLinkTapped: (
        recordId: Long,
        templateId: Long,
        profileJson: String,
        profileMinedAtEpochMs: Long,
        preview: TemplateVariabilityPreviewContent,
    ) -> Unit,
) {
    val title = dialogHandle.title
    val titleHint = dialogHandle.titleHint
    val showRunAIButton = (dialogHandle as? DialogHandle.RecordDetailsDialog.Edit)
        ?.showRunAIButton
        ?: false
    val images: List<String> = dialogHandle.images
    val analysis = (dialogHandle as? DialogHandle.RecordDetailsDialog.Edit)
        ?.recognisedFood
    val showProgressIndicator = (dialogHandle as? DialogHandle.RecordDetailsDialog.Edit)
        ?.showProgressIndicator
    val description = dialogHandle.description
    val nutrientBreakdown = (dialogHandle as? DialogHandle.RecordDetailsDialog.View)
        ?.nutrientBreakdown
    val allowEdit = (dialogHandle as? DialogHandle.RecordDetailsDialog.View)
        ?.allowEdit
        ?: true
    val variabilityProfileJson = (dialogHandle as? DialogHandle.RecordDetailsDialog.View)?.variabilityProfileJson
    val variabilityProfileMinedAtEpochMs =
        (dialogHandle as? DialogHandle.RecordDetailsDialog.View)?.variabilityProfileMinedAtEpochMs ?: 0L
    val recordIdForVariability = (dialogHandle as? DialogHandle.RecordDetailsDialog.View)?.recordId
    val templateDbIdForVariability = (dialogHandle as? DialogHandle.RecordDetailsDialog.View)?.templateDbId
    val templateVariabilityPreview = when (dialogHandle) {
        is DialogHandle.RecordDetailsDialog.View -> dialogHandle.templateVariabilityPreview
        is DialogHandle.RecordDetailsDialog.Edit -> dialogHandle.templateVariabilityPreview
    }

    val saveButtonText =
        if (dialogHandle is DialogHandle.RecordDetailsDialog.View) "Update and Analyze" else "Add and Analyze"

    val showKeyboardOnOpen = remember(dialogHandle) {
        when (dialogHandle) {
            is DialogHandle.RecordDetailsDialog.Edit -> true
            is DialogHandle.RecordDetailsDialog.View -> false
        }
    }

    ScrollableContentDialog(
        onDismissRequested = onDismissRequested,
        content = {
            RecordDetailsDialogContent(
                onTitleChanged = onTitleChanged,
                onDescriptionChanged = onDescriptionChanged,
                showKeyboardOnOpen = showKeyboardOnOpen,
                images = images,
                title = title,
                showRunAIButton = showRunAIButton,
                titleHint = titleHint,
                analysis = analysis,
                showProgressIndicator = showProgressIndicator ?: false,
                description = description,
                titleErrorMessage = dialogHandle.titleValidationError,
                allowEdit = allowEdit,
                nutrientBreakdown = nutrientBreakdown,
                onImageTapped = onImageTapped,
                onImageDeleteTapped = onImageDeleteTapped,
                onAddImageViaCameraTapped = onAddImageViaCameraTapped,
                onAddImageViaPickerTapped = onAddImageViaPickerTapped,
                onImagesInfoButtonTapped = onImagesInfoButtonTapped,
                onRunAIButtonTapped = onRunAIButtonTapped,
                templateVariabilityPreview = templateVariabilityPreview,
                variabilityProfileJson = variabilityProfileJson,
                variabilityProfileMinedAtEpochMs = variabilityProfileMinedAtEpochMs,
                recordIdForVariability = recordIdForVariability,
                templateDbIdForVariability = templateDbIdForVariability,
                onVariabilityDifferentMealLinkTapped = onVariabilityDifferentMealLinkTapped,
            )
        },
        errorMessages = errorMessages,
        buttons = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingDefault),
                horizontalArrangement = Arrangement.End,
            ) {
                if (allowEdit) {
                    TextButton(onDismissRequested) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = PaddingDefault),
                            color = MaterialTheme.colorScheme.primary,
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    TextButton(onSubmitButtonTapped) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = PaddingDefault),
                            color = MaterialTheme.colorScheme.primary,
                            text = saveButtonText,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                } else {
                    TextButton(onDismissRequested) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = PaddingDefault),
                            color = MaterialTheme.colorScheme.primary,
                            text = "Close",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun ColumnScope.RecordDetailsDialogContent(
    onTitleChanged: (TextFieldValue) -> Unit,
    onDescriptionChanged: (TextFieldValue) -> Unit,
    showKeyboardOnOpen: Boolean,
    images: List<String>,
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
    templateVariabilityPreview: TemplateVariabilityPreviewContent?,
    variabilityProfileJson: String?,
    variabilityProfileMinedAtEpochMs: Long,
    recordIdForVariability: Long?,
    templateDbIdForVariability: Long?,
    onVariabilityDifferentMealLinkTapped: (
        recordId: Long,
        templateId: Long,
        profileJson: String,
        profileMinedAtEpochMs: Long,
        preview: TemplateVariabilityPreviewContent,
    ) -> Unit,
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
        showAddPhotoButtons = allowEdit,
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

        Spacer(
            modifier = Modifier
                .height(PaddingDefault)
        )
    }

    val preview = templateVariabilityPreview
    val archetypeLabel = preview?.archetypePickerLabel?.takeIf { it.isNotBlank() }
    if (
        preview != null &&
        preview.slots.isNotEmpty() &&
        archetypeLabel != null &&
        recordIdForVariability != null &&
        templateDbIdForVariability != null &&
        !variabilityProfileJson.isNullOrBlank()
    ) {
        Spacer(modifier = Modifier.height(PaddingDefault))
        TextButton(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(bottom = PaddingDefault),
            onClick = {
                onVariabilityDifferentMealLinkTapped(
                    recordIdForVariability,
                    templateDbIdForVariability,
                    variabilityProfileJson,
                    variabilityProfileMinedAtEpochMs,
                    preview,
                )
            },
        ) {
            Text(
                text = "Looking for a different $archetypeLabel?",
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = TextDecoration.Underline,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreviewView() {
    ViewPreviewContext {
        RecordDetailsDialog(
            dialogHandle = DialogHandle.RecordDetailsDialog.View(
                recordId = 1L,
                templateDbId = 1L,
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
                templateVariabilityPreview = TemplateVariabilityPreviewContent(
                    bannerText = "Pick a variant per slot (demo — not saved yet).",
                    archetypePickerLabel = "Toast breakfast",
                    slots = listOf(
                        TemplateVariabilitySlotPreview(
                            archetypeKey = "toast",
                            archetypeDisplayName = "Toast breakfast",
                            slotKey = "spread",
                            roleDisplayName = "Spread",
                            variants = listOf(
                                TemplateVariabilityVariantPreview("butter", "Butter"),
                                TemplateVariabilityVariantPreview("jam", "Jam"),
                            ),
                        ),
                    ),
                ),
                variabilityProfileJson = "{}",
                variabilityProfileMinedAtEpochMs = 0L,
            ),
            errorMessages = emptyFlow(),
            onTitleChanged = {},
            onDescriptionChanged = {},
            onSubmitButtonTapped = {},
            onImageTapped = {},
            onImageDeleteTapped = {},
            onAddImageViaCameraTapped = {},
            onAddImageViaPickerTapped = {},
            onDismissRequested = {},
            onImagesInfoButtonTapped = {},
            onRunAIButtonTapped = {},
            onVariabilityDifferentMealLinkTapped = { _, _, _, _, _ -> },
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreviewSuggestion() {
    ViewPreviewContext {
        RecordDetailsDialog(
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
            errorMessages = emptyFlow(),
            onTitleChanged = {},
            onDescriptionChanged = {},
            onSubmitButtonTapped = {},
            onImageTapped = {},
            onImageDeleteTapped = {},
            onAddImageViaCameraTapped = {},
            onAddImageViaPickerTapped = {},
            onDismissRequested = {},
            onImagesInfoButtonTapped = {},
            onRunAIButtonTapped = {},
            onVariabilityDifferentMealLinkTapped = { _, _, _, _, _ -> },
        )
    }
}


@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreview() {
    ViewPreviewContext {
        RecordDetailsDialog(
            dialogHandle = DialogHandle.RecordDetailsDialog.Edit(
                title = TextFieldValue(),
                titleHint = "What did you eat?",
                description = TextFieldValue(),
                images = listOf("1", "2"),
                showProgressIndicator = true,
                recognisedFood = null,
            ),
            errorMessages = emptyFlow(),
            onTitleChanged = {},
            onDescriptionChanged = {},
            onSubmitButtonTapped = {},
            onImageTapped = {},
            onImageDeleteTapped = {},
            onAddImageViaCameraTapped = {},
            onAddImageViaPickerTapped = {},
            onDismissRequested = {},
            onImagesInfoButtonTapped = {},
            onRunAIButtonTapped = {},
            onVariabilityDifferentMealLinkTapped = { _, _, _, _, _ -> },
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreviewError() {
    ViewPreviewContext {
        RecordDetailsDialog(
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
            errorMessages = emptyFlow(),
            onTitleChanged = {},
            onDescriptionChanged = {},
            onSubmitButtonTapped = {},
            onImageTapped = {},
            onImageDeleteTapped = {},
            onAddImageViaCameraTapped = {},
            onAddImageViaPickerTapped = {},
            onDismissRequested = {},
            onImagesInfoButtonTapped = {},
            onRunAIButtonTapped = {},
            onVariabilityDifferentMealLinkTapped = { _, _, _, _, _ -> },
        )
    }
}
