package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.features.modal.R
import dev.gaborbiro.dailymacros.features.common.R as CommonR
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import kotlinx.coroutines.delay

@Composable
fun ColumnScope.RecordDetailsCreateView(
    onTitleChanged: (TextFieldValue) -> Unit,
    onDescriptionChanged: (TextFieldValue) -> Unit,
    showKeyboardOnOpen: Boolean,
    imageFilenames: List<String>,
    title: TextFieldValue,
    showRunAIButton: Boolean,
    titleHint: String,
    showProgressIndicator: Boolean,
    description: TextFieldValue,
    titleErrorMessage: String?,
    onImageTapped: (String) -> Unit,
    onImageDeleteTapped: (String) -> Unit,
    onImageMoveLeftTapped: (String) -> Unit,
    onImageMoveRightTapped: (String) -> Unit,
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
            .padding(top = 12.dp)
            .padding(bottom = 12.dp),
        showAddPhotoButtons = imageFilenames.size < 5,
        showImageDeleteButton = true,
        showImageReorderButtons = true,
        showInfoButton = true,
        imageFilenames = imageFilenames,
        onImageTapped = onImageTapped,
        onImageDeleteTapped = onImageDeleteTapped,
        onImageMoveLeftTapped = onImageMoveLeftTapped,
        onImageMoveRightTapped = onImageMoveRightTapped,
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
                                painter = painterResource(CommonR.drawable.ic_chatgpt),
                                contentDescription = "Run image recognition",
                            )
                        }
                    }
                }
            }
        },
        placeholder = {
            Text(
                text = if (showProgressIndicator) "Running image recognition..." else titleHint,
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
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun RecordDetailsCreateViewPreview() {
    ViewPreviewContext {
        RecordDetailsCreateView(
            onTitleChanged = {},
            onDescriptionChanged = {},
            showKeyboardOnOpen = false,
            imageFilenames = listOf("1", "2"),
            title = TextFieldValue(),
            showRunAIButton = false,
            titleHint = "What did you eat?",
            showProgressIndicator = true,
            description = TextFieldValue(),
            titleErrorMessage = null,
            onImageTapped = {},
            onImageDeleteTapped = {},
            onImageMoveLeftTapped = {},
            onImageMoveRightTapped = {},
            onAddImageViaCameraTapped = {},
            onAddImageViaPickerTapped = {},
            onImagesInfoButtonTapped = {},
            onRunAIButtonTapped = {},
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun RecordDetailsCreateViewPreviewRunAi() {
    ViewPreviewContext {
        RecordDetailsCreateView(
            onTitleChanged = {},
            onDescriptionChanged = {},
            showKeyboardOnOpen = false,
            imageFilenames = listOf("1", "2"),
            title = TextFieldValue(),
            showRunAIButton = true,
            titleHint = "Title",
            showProgressIndicator = false,
            description = TextFieldValue(),
            titleErrorMessage = null,
            onImageTapped = {},
            onImageDeleteTapped = {},
            onImageMoveLeftTapped = {},
            onImageMoveRightTapped = {},
            onAddImageViaCameraTapped = {},
            onAddImageViaPickerTapped = {},
            onImagesInfoButtonTapped = {},
            onRunAIButtonTapped = {},
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun RecordDetailsCreateViewPreviewError() {
    ViewPreviewContext {
        RecordDetailsCreateView(
            onTitleChanged = {},
            onDescriptionChanged = {},
            showKeyboardOnOpen = false,
            imageFilenames = listOf("1", "2"),
            title = TextFieldValue(),
            showRunAIButton = true,
            titleHint = "Title",
            showProgressIndicator = false,
            description = TextFieldValue(),
            titleErrorMessage = "error",
            onImageTapped = {},
            onImageDeleteTapped = {},
            onImageMoveLeftTapped = {},
            onImageMoveRightTapped = {},
            onAddImageViaCameraTapped = {},
            onAddImageViaPickerTapped = {},
            onImagesInfoButtonTapped = {},
            onRunAIButtonTapped = {},
        )
    }
}
