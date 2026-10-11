package dev.gaborbiro.dailymacros.features.modal.views

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!showProgressIndicator) {
                    VoiceInputButton(
                        onTextRecognised = { spoken ->
                            onTitleChanged(appendSpokenText(title, spoken))
                        },
                    )
                }
                if (showProgressIndicator || showRunAIButton) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            showProgressIndicator -> {
                                val infiniteTransition = rememberInfiniteTransition(label = "aiIconRotation")
                                val rotation by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 1200, easing = LinearEasing),
                                    ),
                                    label = "aiIconRotationAngle",
                                )
                                Icon(
                                    painter = painterResource(CommonR.drawable.ic_ai_borg),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(25.dp)
                                        .graphicsLayer { rotationZ = rotation },
                                    tint = MaterialTheme.colorScheme.primary,
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
                                        painter = painterResource(CommonR.drawable.ic_ai_borg),
                                        contentDescription = stringResource(R.string.modal_content_run_ai_cd),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        placeholder = {
            Text(
                text = if (showProgressIndicator) stringResource(R.string.modal_content_running_ai_placeholder) else titleHint,
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

/**
 * Mic button that hands off to the device's own speech recogniser (on most phones that's
 * Google's, the same engine as Gboard voice typing) via [RecognizerIntent]. The recogniser
 * app owns the microphone, so this app needs no RECORD_AUDIO permission and no audio ever
 * passes through it. Hidden when the device has no recogniser installed.
 */
@Composable
private fun VoiceInputButton(
    onTextRecognised: (String) -> Unit,
) {
    val context = LocalContext.current
    val prompt = stringResource(R.string.modal_content_voice_input_prompt)
    val intent = remember(prompt) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
    }
    val isAvailable = remember(intent) {
        intent.resolveActivity(context.packageManager) != null
    }
    if (!isAvailable) return

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let(onTextRecognised)
    }

    IconButton(
        onClick = {
            try {
                launcher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                // Recogniser was uninstalled/disabled since we checked; nothing to do.
            }
        },
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = stringResource(R.string.modal_content_voice_input_cd),
        )
    }
}

/**
 * Appends [spoken] to whatever is already typed (so you can dictate in several goes) and puts
 * the cursor at the end. The recogniser returns lower-case text, so the first letter is
 * capitalised when it starts the field.
 */
internal fun appendSpokenText(current: TextFieldValue, spoken: String): TextFieldValue {
    val trimmed = spoken.trim()
    val existing = current.text.trimEnd()
    val newText = if (existing.isEmpty()) {
        trimmed.replaceFirstChar { it.uppercaseChar() }
    } else {
        "$existing $trimmed"
    }
    return TextFieldValue(text = newText, selection = TextRange(newText.length))
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
            titleHint = "Title (optional)",
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
            titleHint = "Title (optional)",
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
