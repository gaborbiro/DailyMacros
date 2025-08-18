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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.DailyMacrosTheme
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import kotlinx.coroutines.delay


@Composable
internal fun InputDialog(
    dialogState: DialogState.InputDialog,
    onRecordDetailsSubmitRequested: (title: String, description: String) -> Unit,
    onRecordDetailsUserTyping: (title: String, description: String) -> Unit,
    onDismissRequested: () -> Unit,
) {
    val title = (dialogState as? DialogState.InputDialog.RecordDetailsDialog)?.title
    val images = (dialogState as? DialogState.InputDialog.RecordDetailsDialog)?.images ?: emptyList()
    val suggestions = (dialogState as? DialogState.InputDialog.CreateWithImageDialog)
        ?.suggestions
    val showProgressIndicator = (dialogState as? DialogState.InputDialog.CreateWithImageDialog)
        ?.showProgressIndicator
    val description =
        (dialogState as? DialogState.InputDialog.RecordDetailsDialog)?.description
    val calories = (dialogState as? DialogState.InputDialog.RecordDetailsDialog)?.calories
    val protein = (dialogState as? DialogState.InputDialog.RecordDetailsDialog)?.protein
    val fat = (dialogState as? DialogState.InputDialog.RecordDetailsDialog)?.fat
    val saturated =
        (dialogState as? DialogState.InputDialog.RecordDetailsDialog)?.ofWhichSaturated
    val carbs = (dialogState as? DialogState.InputDialog.RecordDetailsDialog)?.carbs
    val sugar = (dialogState as? DialogState.InputDialog.RecordDetailsDialog)?.ofWhichSugar
    val salt = (dialogState as? DialogState.InputDialog.RecordDetailsDialog)?.salt
    val fibre = (dialogState as? DialogState.InputDialog.RecordDetailsDialog)?.fibre
    val notes = (dialogState as? DialogState.InputDialog.RecordDetailsDialog)?.notes

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

    ScrollableContentDialog(
        onDismissRequested = onDismissRequested,
        content = {
            InputDialogContent(
                onChange = onRecordDetailsUserTyping,
                images = images,
                titleField = titleField,
                suggestions = suggestions,
                showProgressIndicator = showProgressIndicator ?: false,
                descriptionField = descriptionField,
                error = dialogState.validationError,
                calories = calories,
                protein = protein,
                fat = fat,
                saturated = saturated,
                carbs = carbs,
                sugar = sugar,
                salt = salt,
                fibre = fibre,
                notes = notes,
            )
        },
        buttons = {
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
        }
    )
}

@Composable
private fun ColumnScope.InputDialogContent(
    onChange: (String, String) -> Unit,
    images: List<String>,
    titleField: MutableState<TextFieldValue>,
    suggestions: DialogState.InputDialog.SummarySuggestions?,
    showProgressIndicator: Boolean,
    descriptionField: MutableState<TextFieldValue>,
    error: String?,
    calories: String?,
    protein: String?,
    fat: String?,
    saturated: String?,
    carbs: String?,
    sugar: String?,
    salt: String?,
    fibre: String?,
    notes: String?,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = PaddingDefault)
            .fillMaxWidth()
            .padding(top = PaddingDefault)
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Describe what you ate or drank",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
    }

    Spacer(
        modifier = Modifier
            .height(PaddingDefault)
    )

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(key1 = Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    TextField(
        modifier = Modifier
            .padding(horizontal = PaddingDefault)
            .fillMaxWidth()
            .wrapContentHeight()
            .focusRequester(focusRequester),
        isError = error.isNullOrBlank().not(),
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = {
            Text(
                text = "Title",
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
                    .padding(horizontal = PaddingDefault)
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
        }

    if (showProgressIndicator) {
        CircularProgressIndicator(
            modifier = Modifier
                .padding(vertical = PaddingDefault)
                .size(25.dp)
                .align(Alignment.CenterHorizontally)
        )
    } else {
        Spacer(
            modifier = Modifier
                .height(PaddingDefault)
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
                text = "Description",
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

    suggestions
        ?.description
        ?.takeIf { it.isNotBlank() }
        ?.let { descriptionSuggestion ->
            Spacer(
                modifier = Modifier
                    .height(PaddingHalf)
            )
            FlowRow(
                modifier = Modifier
                    .padding(horizontal = PaddingDefault)
            ) {
                PillLabel(
                    modifier = Modifier
                        .padding(bottom = 4.dp),
                    text = descriptionSuggestion,
                    onClick = {
                        descriptionField.value =
                            descriptionField.value.copy(text = descriptionSuggestion, selection = TextRange(descriptionSuggestion.length))
                        onChange(titleField.value.text, descriptionField.value.text)
                    },
                )
            }
        }

    Macros(
        calories = calories,
        protein = protein,
        fat = fat,
        saturated = saturated,
        carbs = carbs,
        sugar = sugar,
        salt = salt,
        fibre = fibre,
        notes = notes,
    )
}

@Composable
private fun Macros(
    calories: String?,
    protein: String?,
    fat: String?,
    saturated: String?,
    carbs: String?,
    sugar: String?,
    salt: String?,
    fibre: String?,
    notes: String?,
) {
    if (calories != null ||
        protein != null ||
        fat != null ||
        saturated != null ||
        carbs != null ||
        sugar != null ||
        salt != null ||
        fibre != null ||
        notes != null
    ) {
        Spacer(
            modifier = Modifier
                .height(PaddingDefault)
        )
    }

    calories?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(top = 4.dp),
            text = calories,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    protein?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(top = 4.dp),
            text = protein,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    fat?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(top = 4.dp),
            text = fat,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    saturated?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(start = 16.dp, top = 4.dp),
            text = saturated,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    carbs?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(top = 4.dp),
            text = carbs,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    sugar?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(start = 16.dp, top = 4.dp),
            text = sugar,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    salt?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(top = 4.dp),
            text = salt,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    fibre?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(top = 4.dp),
            text = fibre,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    notes?.let {
        PillLabel(
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .padding(top = 4.dp),
            text = notes,
            contentColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            border = null,
            elevation = 0.dp,
        )
    }

    if (calories != null ||
        protein != null ||
        fat != null ||
        saturated != null ||
        carbs != null ||
        sugar != null ||
        salt != null ||
        fibre != null ||
        notes != null
    ) {
        Spacer(
            modifier = Modifier
                .height(PaddingDefault)
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreview() {
    DailyMacrosTheme {
        InputDialog(
            dialogState = DialogState.InputDialog.CreateWithImageDialog(
                image = null,
                showProgressIndicator = true,
                suggestions = null,
            ),
            onRecordDetailsSubmitRequested = { _, _ -> },
            onRecordDetailsUserTyping = { _, _ -> },
            onDismissRequested = {},
        )
    }
}


@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreviewEdit() {
    DailyMacrosTheme {
        InputDialog(
            dialogState = DialogState.InputDialog.RecordDetailsDialog(
                recordId = 1L,
                images = emptyList(),
                titleSuggestionProgressIndicator = true,
                titleSuggestions = emptyList(),
                title = "This is a title",
                description = "This is a description",
                calories = "Calories: 2100 cal",
                protein = "Protein: 150g",
                fat = "Fat 100g",
                ofWhichSaturated = "of which saturated: 20g",
                carbs = "Carbs: 100g",
                ofWhichSugar = "of which sugars: 30g",
                salt = "Salt: 5g",
                fibre = "Fibre: 4.5g",
                notes = "Notes: This is a note",
            ),
            onRecordDetailsSubmitRequested = { _, _ -> },
            onRecordDetailsUserTyping = { _, _ -> },
            onDismissRequested = {},
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreviewSuggestion() {
    DailyMacrosTheme {
        InputDialog(
            dialogState = DialogState.InputDialog.CreateWithImageDialog(
                image = null,
                suggestions = DialogState.InputDialog.SummarySuggestions(
                    titles = listOf("This is a title suggestion", "This is another title suggestion"),
                    description = "",
                ),
            ),
            onRecordDetailsSubmitRequested = { _, _ -> },
            onRecordDetailsUserTyping = { _, _ -> },
            onDismissRequested = {},
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreviewError() {
    DailyMacrosTheme {
        InputDialog(
            dialogState = DialogState.InputDialog.CreateWithImageDialog(
                image = null,
                suggestions = DialogState.InputDialog.SummarySuggestions(
                    titles = listOf("This is a title suggestion", "This is another title suggestion"),
                    description = "This ready meal contains curry of beef (caril de vitela), basmati rice, leeks, and carrots. It is labeled as medium size (250g) and high in carbohydrates. The dish also contains tomato pulp, onion, olive oil, curry spice blend, celery, turmeric, and salt.",
                ),
                validationError = "error"
            ),
            onRecordDetailsSubmitRequested = { _, _ -> },
            onRecordDetailsUserTyping = { _, _ -> },
            onDismissRequested = {},
        )
    }
}
