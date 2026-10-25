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

private const val SILENCE_TIMEOUT_MILLIS = 5_000L

/**
 * Mic button that hands off to the device's own speech recogniser (on most phones that's
 * Google's, the same engine as Gboard voice typing) via [RecognizerIntent]. The recogniser
 * app owns the microphone, so this app needs no RECORD_AUDIO permission and no audio ever
 * passes through it. Hidden when the device has no recogniser installed.
 *
 * Only reports upward: [onTextRecognised] gets the raw recognised text (the ViewModel decides how
 * it joins what's typed), and [onLaunchFailed] gets the error if the recogniser disappeared
 * between the availability check and the tap.
 */
@Composable
internal fun VoiceInputButton(
    prompt: String,
    contentDescription: String,
    onTextRecognised: (String) -> Unit,
    onLaunchFailed: (Throwable) -> Unit,
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
            } catch (e: ActivityNotFoundException) {
                // Recogniser was uninstalled/disabled since the availability check.
                onLaunchFailed(e)
            }
        },
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = contentDescription,
        )
    }
}
