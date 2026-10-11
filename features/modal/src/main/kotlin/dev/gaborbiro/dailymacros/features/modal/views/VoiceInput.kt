package dev.gaborbiro.dailymacros.features.modal.views

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

private const val SILENCE_TIMEOUT_MILLIS = 5_000L

/**
 * Mic button that hands off to the device's own speech recogniser (on most phones that's
 * Google's, the same engine as Gboard voice typing) via [RecognizerIntent]. The recogniser
 * app owns the microphone, so this app needs no RECORD_AUDIO permission and no audio ever
 * passes through it. Hidden when the device has no recogniser installed.
 */
@Composable
internal fun VoiceInputButton(
    prompt: String,
    contentDescription: String,
    onTextRecognised: (String) -> Unit,
) {
    val context = LocalContext.current
    val intent = remember(prompt) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            // Wait longer through pauses while you think of the next item. These are hints:
            // the recogniser app decides whether to honour them.
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_TIMEOUT_MILLIS)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_TIMEOUT_MILLIS)
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
            contentDescription = contentDescription,
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
