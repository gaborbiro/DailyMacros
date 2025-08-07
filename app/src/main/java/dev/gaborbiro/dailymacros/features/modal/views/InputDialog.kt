package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.Dialog
import dev.gaborbiro.dailymacros.design.NotesTheme
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingDouble
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import kotlinx.coroutines.delay


@Composable
fun InputDialog(
    dialogState: DialogState.InputDialog,
    onDialogDismissed: () -> Unit,
    onRecordDetailsSubmitRequested: (String, String) -> Unit,
    onRecordDetailsUserTyping: (String, String) -> Unit,
) {
    Dialog(
        onDismissRequest = {
            onDialogDismissed()
        },
    ) {
//            val image = (dialogState as? DialogState.InputDialogState.Edit)?.image
        val title = (dialogState as? DialogState.InputDialog.Edit)?.title
        val titleSuggestion = (dialogState as? DialogState.InputDialog.CreateWithImage)
            ?.let { it.titleSuggestions to it.titleSuggestionProgressIndicator }
        val description = (dialogState as? DialogState.InputDialog.Edit)?.description
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 4.dp,
            modifier = Modifier.Companion
                .wrapContentHeight()
        ) {
            InputDialogContent(
                onCancel = {
                    onDialogDismissed()
                },
                onSubmit = { title, description ->
                    onRecordDetailsSubmitRequested(title, description)
                },
                onChange = { title, description ->
                    onRecordDetailsUserTyping(title, description)
                },
                title = title,
                titleSuggestions = titleSuggestion?.first ?: emptyList(),
                titleSuggestionProgressIndicator = titleSuggestion?.second ?: false,
                description = description,
                error = dialogState.validationError,
            )
        }
    }
}


@Composable
fun InputDialogContent(
    onCancel: () -> Unit,
    onSubmit: (String, String) -> Unit,
    onChange: (String, String) -> Unit,
    title: String? = null,
    titleSuggestions: List<String>,
    titleSuggestionProgressIndicator: Boolean,
    description: String? = null,
    error: String?,
) {
    val focusRequester = remember { FocusRequester() }
    var titleFieldValue by remember {
        mutableStateOf(TextFieldValue(title ?: ""))
    }
    var descriptionFieldValue by remember {
        mutableStateOf(TextFieldValue(description ?: ""))
    }

    val onDone: () -> Unit = {
        onSubmit(titleFieldValue.text.trim(), descriptionFieldValue.text.trim())
    }

    Column(
        modifier = Modifier.padding(PaddingDefault),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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

        Spacer(modifier = Modifier.height(PaddingDefault))

        TextField(
            modifier = Modifier
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
            value = titleFieldValue,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            onValueChange = {
                titleFieldValue = it
                onChange(titleFieldValue.text, descriptionFieldValue.text)
            },
        )
        if (error.isNullOrBlank().not()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .height(16.dp),
            )
        }

        titleSuggestions.takeIf { it.isNotEmpty() }?.let {
            Spacer(modifier = Modifier.height(PaddingHalf))
            FlowRow {
                it.forEach {
                    PillLabel(
                        modifier = Modifier
                            .padding(bottom = 4.dp),
                        text = it,
                        onClick = {
                            titleFieldValue =
                                titleFieldValue.copy(text = it, selection = TextRange(it.length))
                            onChange(titleFieldValue.text, descriptionFieldValue.text)
                        },
                    )
                }
            }
        }

        if (titleSuggestionProgressIndicator) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(vertical = 16.dp)
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
                .height(96.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            value = descriptionFieldValue,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
            ),
            onValueChange = {
                descriptionFieldValue = it
                onChange(titleFieldValue.text, descriptionFieldValue.text)
            },
        )

        Spacer(modifier = Modifier.height(PaddingDouble))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onCancel) {
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

        LaunchedEffect(key1 = Unit) {
            delay(100)
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun PillLabel(
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
    NotesTheme {
        InputDialog(
            dialogState = DialogState.InputDialog.CreateWithImage(
                image = null,
                titleSuggestionProgressIndicator = true,
                titleSuggestions = emptyList(),
            ),
            onDialogDismissed = {},
            onRecordDetailsSubmitRequested = { _, _ -> },
            onRecordDetailsUserTyping = { _, _ -> },
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreviewSuggestion() {
    NotesTheme {
        InputDialog(
            dialogState = DialogState.InputDialog.CreateWithImage(
                image = null,
                titleSuggestions = listOf("This is a title suggestion", "This is another title suggestion"),
            ),
            onDialogDismissed = {},
            onRecordDetailsSubmitRequested = { _, _ -> },
            onRecordDetailsUserTyping = { _, _ -> },
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoteInputDialogContentPreviewError() {
    NotesTheme {
        InputDialog(
            dialogState = DialogState.InputDialog.CreateWithImage(
                image = null,
                titleSuggestions = listOf("This is a title suggestion", "This is another title suggestion"),
                validationError = "error"
            ),
            onDialogDismissed = {},
            onRecordDetailsSubmitRequested = { _, _ -> },
            onRecordDetailsUserTyping = { _, _ -> },
        )
    }
}
