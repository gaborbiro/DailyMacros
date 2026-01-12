package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.design.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.common.views.PreviewImageStoreProvider
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import dev.gaborbiro.dailymacros.features.modal.model.MacrosUIModel
import dev.gaborbiro.dailymacros.features.modal.model.SummarySuggestions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow


@Composable
internal fun RecordDetailsDialog(
    dialogState: DialogState.RecordDetailsDialog,
    errorMessages: Flow<String>,
    onTitleSuggestionSelected: (String) -> Unit,
    onDescriptionSuggestionSelected: (String) -> Unit,
    onTitleChanged: (TextFieldValue) -> Unit,
    onDescriptionChanged: (TextFieldValue) -> Unit,
    onSubmitButtonTapped: () -> Unit,
    onImageTapped: (String) -> Unit,
    onImageDeleteTapped: (String) -> Unit,
    onAddImageViaCameraTapped: () -> Unit,
    onAddImageViaPickerTapped: () -> Unit,
    onDismissRequested: () -> Unit,
    onImagesInfoButtonTapped: () -> Unit,
) {
    val title = dialogState.title
    val titleHint = dialogState.titleHint
    val images: List<String> = dialogState.images
    val suggestions = (dialogState as? DialogState.RecordDetailsDialog.Edit)
        ?.suggestions
    val showProgressIndicator = (dialogState as? DialogState.RecordDetailsDialog.Edit)
        ?.showProgressIndicator
    val description = dialogState.description
    val macros = (dialogState as? DialogState.RecordDetailsDialog.View)
        ?.macros
    val allowEdit = (dialogState as? DialogState.RecordDetailsDialog.View)
        ?.allowEdit
        ?: true

    val saveButtonText =
        if (dialogState is DialogState.RecordDetailsDialog.View) "Update and Analyze" else "Add and Analyze"

    val showKeyboardOnOpen = remember(dialogState) {
        when (dialogState) {
            is DialogState.RecordDetailsDialog.Edit -> true
            is DialogState.RecordDetailsDialog.View -> false
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
                titleHint = titleHint,
                suggestions = suggestions,
                onTitleSuggestionSelected = onTitleSuggestionSelected,
                onDescriptionSuggestionSelected = onDescriptionSuggestionSelected,
                showProgressIndicator = showProgressIndicator ?: false,
                description = description,
                titleErrorMessage = dialogState.titleValidationError,
                allowEdit = allowEdit,
                macros = macros,
                onImageTapped = onImageTapped,
                onImageDeleteTapped = onImageDeleteTapped,
                onAddImageViaCameraTapped = onAddImageViaCameraTapped,
                onAddImageViaPickerTapped = onAddImageViaPickerTapped,
                onImagesInfoButtonTapped = onImagesInfoButtonTapped,
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
    onTitleSuggestionSelected: (String) -> Unit,
    onDescriptionSuggestionSelected: (String) -> Unit,
    showKeyboardOnOpen: Boolean,
    images: List<String>,
    title: TextFieldValue,
    titleHint: String,
    suggestions: SummarySuggestions?,
    showProgressIndicator: Boolean,
    description: TextFieldValue,
    titleErrorMessage: String?,
    allowEdit: Boolean,
    macros: MacrosUIModel?,
    onImageTapped: (String) -> Unit,
    onImageDeleteTapped: (String) -> Unit,
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
        enabled = allowEdit,
        isError = titleErrorMessage.isNullOrBlank().not(),
        textStyle = MaterialTheme.typography.bodyMedium,
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
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .alpha(.3f),
                    painter = painterResource(R.drawable.ic_chatgpt),
                    contentDescription = "chatgpt",
                )
                Text(
                    text = "suggests:",
                    style = MaterialTheme.typography.labelLarge,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray,
                )
                it.forEach {
                    PillLabel(
                        modifier = Modifier
                            .padding(bottom = 4.dp),
                        text = it,
                        onClick = {
                            onTitleSuggestionSelected(it)
                        },
                    )
                }
            }
        }

    if (showProgressIndicator) {
        Text(
            modifier = Modifier
                .padding(horizontal = PaddingDefault, vertical = PaddingHalf)
                .fillMaxWidth(),
            text = "Analyzing images…",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        CircularProgressIndicator(
            modifier = Modifier
                .size(25.dp)
                .align(Alignment.CenterHorizontally)
        )
    }

    Spacer(
        modifier = Modifier
            .height(PaddingDefault)
    )

    ImageStrip(
        modifier = Modifier
            .fillMaxWidth()
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

    suggestions
        ?.description
        ?.takeIf { it.isNotBlank() }
        ?.let { descriptionSuggestion ->
            Box {
                PillLabel(
                    modifier = Modifier
                        .padding(horizontal = PaddingDefault)
                        .padding(top = PaddingHalf),
                    text = descriptionSuggestion,
                    onClick = {
                        onDescriptionSuggestionSelected(descriptionSuggestion)
                    },
                    iconOrientation = Orientation.Vertical,
                )
                Icon(
                    modifier = Modifier
                        .padding(end = PaddingDefault + 8.dp, bottom = 8.dp)
                        .size(24.dp)
                        .alpha(.3f)
                        .align(Alignment.BottomEnd),
                    painter = painterResource(R.drawable.ic_chatgpt),
                    contentDescription = "chatgpt",
                )
            }
        }

    macros?.let {
        Spacer(
            modifier = Modifier
                .height(PaddingDefault)
        )
        MacroTable(macros = macros)
        Spacer(
            modifier = Modifier
                .height(PaddingDefault)
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreviewEdit() {
    ViewPreviewContext {
        PreviewImageStoreProvider {
            RecordDetailsDialog(
                dialogState = DialogState.RecordDetailsDialog.View(
                    recordId = 1L,
                    title = TextFieldValue(),
                    titleHint = "Describe your meal (or pick a suggestion from below)",
                    description = TextFieldValue(),
                    images = listOf("1", "2"),
                    allowEdit = true,
                    macros = MacrosUIModel(
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
                ),
                errorMessages = emptyFlow(),
                onTitleSuggestionSelected = {},
                onDescriptionSuggestionSelected = {},
                onTitleChanged = {},
                onDescriptionChanged = {},
                onSubmitButtonTapped = {},
                onImageTapped = {},
                onImageDeleteTapped = {},
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
private fun NoteInputDialogContentPreviewSuggestion() {
    ViewPreviewContext {
        PreviewImageStoreProvider {
            RecordDetailsDialog(
                dialogState = DialogState.RecordDetailsDialog.Edit(
                    title = TextFieldValue(),
                    titleHint = "Describe your meal",
                    description = TextFieldValue(),
                    images = listOf("1", "2"),
                    suggestions = SummarySuggestions(
                        titles = listOf("This is a title suggestion", "This is another title suggestion"),
                        description = "This ready meal contains curry of beef (caril de vitela), basmati rice, leeks, and carrots. It is labeled as medium size (250g) and high in carbohydrates. The dish also contains tomato pulp, onion, olive oil, curry spice blend, celery, turmeric, and salt.",
                    ),
                ),
                errorMessages = emptyFlow(),
                onTitleSuggestionSelected = {},
                onDescriptionSuggestionSelected = {},
                onTitleChanged = {},
                onDescriptionChanged = {},
                onSubmitButtonTapped = {},
                onImageTapped = {},
                onImageDeleteTapped = {},
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
    ViewPreviewContext {
        PreviewImageStoreProvider {
            RecordDetailsDialog(
                dialogState = DialogState.RecordDetailsDialog.Edit(
                    title = TextFieldValue(),
                    titleHint = "Describe your meal (or pick a suggestion from below)",
                    description = TextFieldValue(),
                    images = listOf("1", "2"),
                    showProgressIndicator = true,
                    suggestions = null,
                ),
                errorMessages = emptyFlow(),
                onTitleSuggestionSelected = {},
                onDescriptionSuggestionSelected = {},
                onTitleChanged = {},
                onDescriptionChanged = {},
                onSubmitButtonTapped = {},
                onImageTapped = {},
                onImageDeleteTapped = {},
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
    ViewPreviewContext {
        PreviewImageStoreProvider {
            RecordDetailsDialog(
                dialogState = DialogState.RecordDetailsDialog.Edit(
                    title = TextFieldValue(),
                    titleHint = "Describe your meal",
                    titleValidationError = "error",
                    description = TextFieldValue(),
                    images = listOf("1", "2"),
                    suggestions = SummarySuggestions(
                        titles = listOf("This is a title suggestion", "This is another title suggestion"),
                        description = "This ready meal contains curry of beef (caril de vitela), basmati rice, leeks, and carrots. It is labeled as medium size (250g) and high in carbohydrates. The dish also contains tomato pulp, onion, olive oil, curry spice blend, celery, turmeric, and salt.",
                    ),
                ),
                errorMessages = emptyFlow(),
                onTitleSuggestionSelected = {},
                onDescriptionSuggestionSelected = {},
                onTitleChanged = {},
                onDescriptionChanged = {},
                onSubmitButtonTapped = {},
                onImageTapped = {},
                onImageDeleteTapped = {},
                onAddImageViaCameraTapped = {},
                onAddImageViaPickerTapped = {},
                onDismissRequested = {},
                onImagesInfoButtonTapped = {},
            )
        }
    }
}
