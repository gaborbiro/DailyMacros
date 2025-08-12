package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
    val title = (dialogState as? DialogState.InputDialog.RecordDetails)?.title
    val titleSuggestion = (dialogState as? DialogState.InputDialog.CreateWithImage)
        ?.let { it.titleSuggestions to it.titleSuggestionProgressIndicator }
    val description =
        (dialogState as? DialogState.InputDialog.RecordDetails)?.description
    val calories = (dialogState as? DialogState.InputDialog.RecordDetails)?.calories
    val carbs = (dialogState as? DialogState.InputDialog.RecordDetails)?.carbs
    val sugar = (dialogState as? DialogState.InputDialog.RecordDetails)?.ofWhichSugar
    val protein = (dialogState as? DialogState.InputDialog.RecordDetails)?.protein
    val fat = (dialogState as? DialogState.InputDialog.RecordDetails)?.fat
    val saturated =
        (dialogState as? DialogState.InputDialog.RecordDetails)?.ofWhichSaturated
    val salt = (dialogState as? DialogState.InputDialog.RecordDetails)?.salt

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
                titleField = titleField,
                titleSuggestions = titleSuggestion?.first ?: emptyList(),
                titleSuggestionProgressIndicator = titleSuggestion?.second ?: false,
                descriptionField = descriptionField,
                error = dialogState.validationError,
                calories = calories,
                protein = protein,
                carbs = carbs,
                sugar = sugar,
                fat = fat,
                saturated = saturated,
                salt = salt,
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
    titleField: MutableState<TextFieldValue>,
    titleSuggestions: List<String>,
    titleSuggestionProgressIndicator: Boolean,
    descriptionField: MutableState<TextFieldValue>,
    error: String?,
    calories: String?,
    protein: String?,
    carbs: String?,
    sugar: String?,
    fat: String?,
    saturated: String?,
    salt: String?,
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
            text = "What did you eat or drink?",
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

    titleSuggestions.takeIf { it.isNotEmpty() }?.let {
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

    if (titleSuggestionProgressIndicator) {
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

    Macros(
        calories = calories,
        protein = protein,
        carbs = carbs,
        sugar = sugar,
        fat = fat,
        saturated = saturated,
        salt = salt,
    )
}

@Composable
private fun Macros(
    calories: String?,
    protein: String?,
    carbs: String?,
    sugar: String?,
    fat: String?,
    saturated: String?,
    salt: String?,
) {
    if (calories != null || protein != null || carbs != null || sugar != null || fat != null || saturated != null || salt != null) {
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

    if (calories != null || protein != null || carbs != null || sugar != null || fat != null || saturated != null || salt != null) {
        Spacer(
            modifier = Modifier
                .height(PaddingDefault)
        )
    }
}

@Composable
private fun PillLabel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    text: String,
    onClick: (() -> Unit)? = null, // if null, it's non-clickable/label-style
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    contentColor: Color = MaterialTheme.colorScheme.primary,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    elevation: Dp = 0.dp,
    enabled: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
) {
    val shape = RoundedCornerShape(50) // pill

    val clickableModifier = if (onClick != null && enabled) {
        Modifier.clickable(
            onClick = onClick,
            indication = LocalIndication.current,
            interactionSource = remember { MutableInteractionSource() }
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .then(clickableModifier),
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        tonalElevation = elevation,
        border = border,
        shadowElevation = elevation
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 24.dp) // make it thinner than default buttons
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = textStyle,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreview() {
    DailyMacrosTheme {
        InputDialog(
            dialogState = DialogState.InputDialog.CreateWithImage(
                image = null,
                titleSuggestionProgressIndicator = true,
                titleSuggestions = emptyList(),
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
            dialogState = DialogState.InputDialog.RecordDetails(
                recordId = 1L,
                image = null,
                titleSuggestionProgressIndicator = true,
                titleSuggestions = emptyList(),
                title = "This is a title",
                description = "This is a description",
                calories = "Calories: 2100 cal",
                protein = "Protein: 150g",
                carbs = "Carbs: 100g",
                ofWhichSugar = "of which sugars: 30g",
                fat = "Fat 100g",
                ofWhichSaturated = "of which saturated: 20g",
                salt = "Salt: 5g",
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
            dialogState = DialogState.InputDialog.CreateWithImage(
                image = null,
                titleSuggestions = listOf("This is a title suggestion", "This is another title suggestion"),
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
            dialogState = DialogState.InputDialog.CreateWithImage(
                image = null,
                titleSuggestions = listOf("This is a title suggestion", "This is another title suggestion"),
                validationError = "error"
            ),
            onRecordDetailsSubmitRequested = { _, _ -> },
            onRecordDetailsUserTyping = { _, _ -> },
            onDismissRequested = {},
        )
    }
}
