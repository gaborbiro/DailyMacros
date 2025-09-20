package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.AppTheme
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.design.PaddingQuarter
import dev.gaborbiro.dailymacros.features.common.view.PreviewImageStoreProvider
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.features.modal.model.MacrosUIModel
import kotlinx.coroutines.delay


@Composable
internal fun InputDialog(
    dialogState: DialogState.InputDialog,
    onRecordDetailsSubmitRequested: (title: String, description: String) -> Unit,
    onRecordDetailsUserTyping: (title: String, description: String) -> Unit,
    onImageTapped: (String) -> Unit,
    onAddImageViaCameraTapped: () -> Unit,
    onAddImageViaPickerTapped: () -> Unit,
    onDismissRequested: () -> Unit,
    onImagesInfoButtonTapped: () -> Unit,
) {
    val title = (dialogState as? DialogState.InputDialog.RecordDetailsDialog)?.title
    val titleHint = dialogState.titleHint
    val images: List<String> =
        (dialogState as? DialogState.InputDialog.RecordDetailsDialog)
            ?.images
            ?: run { (dialogState as? DialogState.InputDialog.CreateWithImageDialog)?.images }
            ?: emptyList()
    val suggestions = (dialogState as? DialogState.InputDialog.CreateWithImageDialog)
        ?.suggestions
    val showProgressIndicator = (dialogState as? DialogState.InputDialog.CreateWithImageDialog)
        ?.showProgressIndicator
    val description =
        (dialogState as? DialogState.InputDialog.RecordDetailsDialog)?.description
    val macros = (dialogState as? DialogState.InputDialog.RecordDetailsDialog)?.macros

    val titleField: MutableState<TextFieldValue> = remember {
        mutableStateOf(TextFieldValue(title ?: ""))
    }
    val descriptionField: MutableState<TextFieldValue> = remember {
        mutableStateOf(TextFieldValue(description ?: ""))
    }
    val onDone: () -> Unit = {
        onRecordDetailsSubmitRequested(
            /* title = */ titleField.value.text.trim(),
            /* description = */ descriptionField.value.text.trim(),
        )
    }

    val showKeyboardOnOpen = remember(dialogState) {
        when (dialogState) {
            is DialogState.InputDialog.CreateDialog -> true
            is DialogState.InputDialog.CreateWithImageDialog -> true
            is DialogState.InputDialog.RecordDetailsDialog -> false
        }
    }

    ScrollableContentDialog(
        onDismissRequested = onDismissRequested,
        content = {
            InputDialogContent(
                onChange = onRecordDetailsUserTyping,
                showKeyboardOnOpen = showKeyboardOnOpen,
                images = images,
                titleField = titleField,
                titleHint = titleHint,
                suggestions = suggestions,
                showProgressIndicator = showProgressIndicator ?: false,
                descriptionField = descriptionField,
                error = dialogState.validationError,
                macros = macros,
                onImageTapped = onImageTapped,
                onAddImageViaCameraTapped = onAddImageViaCameraTapped,
                onAddImageViaPickerTapped = onAddImageViaPickerTapped,
                onImagesInfoButtonTapped = onImagesInfoButtonTapped,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onDismissRequested) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = PaddingDefault),
                        color = MaterialTheme.colorScheme.primary,
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                TextButton(onDone) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = PaddingDefault),
                        color = MaterialTheme.colorScheme.primary,
                        text = "Save",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
        buttons = {

        }
    )
}

@Composable
private fun ColumnScope.InputDialogContent(
    onChange: (String, String) -> Unit,
    showKeyboardOnOpen: Boolean,
    images: List<String>,
    titleField: MutableState<TextFieldValue>,
    titleHint: String,
    suggestions: DialogState.InputDialog.SummarySuggestions?,
    showProgressIndicator: Boolean,
    descriptionField: MutableState<TextFieldValue>,
    error: String?,
    macros: MacrosUIModel?,
    onImageTapped: (String) -> Unit,
    onAddImageViaCameraTapped: () -> Unit,
    onAddImageViaPickerTapped: () -> Unit,
    onImagesInfoButtonTapped: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    if (showKeyboardOnOpen) {
        LaunchedEffect(key1 = Unit) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    TextField(
        modifier = Modifier
            .padding(top = PaddingDefault)
            .padding(horizontal = PaddingDefault)
            .fillMaxWidth()
            .wrapContentHeight()
            .focusRequester(focusRequester),
        isError = error.isNullOrBlank().not(),
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = {
            Text(
                text = titleHint,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        value = titleField.value,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next
        ),
        onValueChange = {
            titleField.value = it
            onChange(titleField.value.text, descriptionField.value.text)
        },
    )
    if (error.isNullOrBlank().not()) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .height(16.dp)
                .padding(horizontal = PaddingDefault),
        )
    }

    suggestions
        ?.titles
        ?.takeIf { it.isNotEmpty() }
        ?.let {
            Spacer(
                modifier = Modifier
                    .height(PaddingHalf)
            )

            FlowRow(
                modifier = Modifier
                    .padding(horizontal = PaddingDefault),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                it.forEach {
                    PillLabel(
                        modifier = Modifier
                            .padding(bottom = 4.dp),
                        text = it,
                        onClick = {
                            titleField.value =
                                titleField.value.copy(text = it, selection = TextRange(it.length))
                            onChange(titleField.value.text, descriptionField.value.text)
                        },
                    )
                }
            }

//            if (titleSelectTooltipEnabled) {
//                Tooltip(
//                    text = "☝\uFE0FSelect an AI suggested title or write your own.\nNote: feel free to add additional photos (for example nutrient label) but keep things focused: one food/dish.",
//                    onDismiss = onTitleSelectTooltipDismissed,
//                )
//            }
        }

    if (showProgressIndicator) {
        Text(
            modifier = Modifier
                .padding(horizontal = PaddingDefault, vertical = PaddingHalf)
                .fillMaxWidth(),
            text = "Analyzing image…",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        CircularProgressIndicator(
            modifier = Modifier
                .padding(8.dp)
                .size(25.dp)
                .align(Alignment.CenterHorizontally)
        )
    } else {
        Spacer(
            modifier = Modifier
                .height(PaddingDefault)
        )
    }

    ImageStrip(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = PaddingDefault),
        images = images,
        onImageTapped = onImageTapped,
        onAddImageViaCameraTapped = onAddImageViaCameraTapped,
        onAddImageViaPickerTapped = onAddImageViaPickerTapped,
        onInfoButtonTapped = onImagesInfoButtonTapped,
    )

    suggestions
        ?.description
        ?.takeIf { it.isNotBlank() }
        ?.let { descriptionSuggestion ->
            Text(
                modifier = Modifier
                    .padding(top = PaddingDefault, bottom = PaddingQuarter)
                    .padding(horizontal = PaddingDefault),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                text = "Verify that the AI can understand your photo:"
            )
            PillLabel(
                modifier = Modifier
                    .padding(horizontal = PaddingDefault)
                    .padding(bottom = PaddingHalf),
                text = descriptionSuggestion,
                onClick = {
                    descriptionField.value =
                        descriptionField.value.copy(text = descriptionSuggestion, selection = TextRange(descriptionSuggestion.length))
                    onChange(titleField.value.text, descriptionField.value.text)
                },
            )
        }

    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaddingDefault)
            .height(96.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = {
            Text(
                text = "Mention unclear components, quantities or other instructions for the AI (for ex. \"I only ate half of it\")",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        value = descriptionField.value,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences,
        ),
        onValueChange = {
            descriptionField.value = it
            onChange(titleField.value.text, descriptionField.value.text)
        },
    )

    macros?.let {
        MacroTable(macros = macros)
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreviewEdit() {
    AppTheme {
        PreviewImageStoreProvider {
            InputDialog(
                dialogState = DialogState.InputDialog.RecordDetailsDialog(
                    recordId = 1L,
                    images = listOf("1", "2"),
                    titleSuggestions = emptyList(),
                    title = null,
                    titleHint = "Describe your meal (or pick a suggestion from below)",
                    description = null,
                    macros = MacrosUIModel(
                        calories = "Calories: 2100 cal",
                        protein = "Protein: 150g",
                        fat = "Fat 100g",
                        ofWhichSaturated = "of which saturated: 20g",
                        carbs = "Carbs: 100g",
                        ofWhichSugar = "of which sugars: 30g",
                        salt = "Salt: 5g",
                        fibre = "Fibre: 4.5g",
                        notes = "Notes: This is a note",
                    )
                ),
                onRecordDetailsSubmitRequested = { _, _ -> },
                onRecordDetailsUserTyping = { _, _ -> },
                onImageTapped = {},
                onAddImageViaCameraTapped = {},
                onAddImageViaPickerTapped = {},
                onDismissRequested = {},
                onImagesInfoButtonTapped = {},
            )
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreview() {
    AppTheme {
        InputDialog(
            dialogState = DialogState.InputDialog.CreateWithImageDialog(
                images = listOf("1", "2"),
                showProgressIndicator = true,
                suggestions = null,
                titleHint = "Describe your meal (or pick a suggestion from below)",
            ),
            onRecordDetailsSubmitRequested = { _, _ -> },
            onRecordDetailsUserTyping = { _, _ -> },
            onImageTapped = {},
            onAddImageViaCameraTapped = {},
            onAddImageViaPickerTapped = {},
            onDismissRequested = {},
            onImagesInfoButtonTapped = {},
        )
    }
}


@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreviewSuggestion() {
    AppTheme {
        PreviewImageStoreProvider {
            InputDialog(
                dialogState = DialogState.InputDialog.CreateWithImageDialog(
                    images = listOf("1", "2"),
                    suggestions = DialogState.InputDialog.SummarySuggestions(
                        titles = listOf("This is a title suggestion", "This is another title suggestion"),
                        description = "",
                    ),
                    titleHint = "Describe your meal",
                ),
                onRecordDetailsSubmitRequested = { _, _ -> },
                onRecordDetailsUserTyping = { _, _ -> },
                onImageTapped = {},
                onAddImageViaCameraTapped = {},
                onAddImageViaPickerTapped = {},
                onDismissRequested = {},
                onImagesInfoButtonTapped = {},
            )
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreviewError() {
    AppTheme {
        InputDialog(
            dialogState = DialogState.InputDialog.CreateWithImageDialog(
                images = listOf("1", "2"),
                suggestions = DialogState.InputDialog.SummarySuggestions(
                    titles = listOf("This is a title suggestion", "This is another title suggestion"),
                    description = "This ready meal contains curry of beef (caril de vitela), basmati rice, leeks, and carrots. It is labeled as medium size (250g) and high in carbohydrates. The dish also contains tomato pulp, onion, olive oil, curry spice blend, celery, turmeric, and salt.",
                ),
                titleHint = "Describe your meal",
                validationError = "error",
            ),
            onRecordDetailsSubmitRequested = { _, _ -> },
            onRecordDetailsUserTyping = { _, _ -> },
            onImageTapped = {},
            onAddImageViaCameraTapped = {},
            onAddImageViaPickerTapped = {},
            onDismissRequested = {},
            onImagesInfoButtonTapped = {},
        )
    }
}
