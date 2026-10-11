package dev.gaborbiro.dailymacros.features.modal.views.dictation

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.gaborbiro.dailymacros.features.modal.R

/**
 * One [DictationController] shared by the fields of a screen, released when the screen goes away
 * or the app is backgrounded (so the mic is never held in the background).
 */
@Composable
internal fun rememberDictationController(): DictationController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember(context) { DictationController(context) }
    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) controller.cancel()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.cancel()
        }
    }
    return controller
}

/**
 * Mic button for a text field. Tap to dictate into [value] in-app (words appear live, pauses don't
 * end it); tap the pulsing stop button to finish. Needs the microphone permission; if it's refused,
 * or no in-app recogniser exists, falls back to the system voice-input dialog, which needs none.
 * Hidden when the device can do neither.
 */
@Composable
internal fun DictationButton(
    controller: DictationController,
    target: DictationTarget,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onDictationStarted: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val fallbackPrompt = stringResource(R.string.modal_content_voice_input_prompt)
    val fallbackIntent = remember(fallbackPrompt) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, fallbackPrompt)
    }
    val fallbackAvailable = remember(fallbackIntent) {
        fallbackIntent.resolveActivity(context.packageManager) != null
    }
    if (!controller.isAvailable && !fallbackAvailable) return

    val fallbackLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { onValueChange(appendSpokenText(value.text, listOf(it))) }
    }

    fun launchFallback() {
        if (!fallbackAvailable) return
        try {
            fallbackLauncher.launch(fallbackIntent)
        } catch (_: ActivityNotFoundException) {
            // Recogniser was uninstalled/disabled since we checked; nothing to do.
        }
    }

    fun startInApp() {
        // Drop the keyboard: frees up room and keeps the IME from editing the field under us.
        focusManager.clearFocus()
        controller.start(
            target = target,
            current = value,
            onText = onValueChange,
            onError = { error -> showDictationError(context, error) },
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startInApp() else launchFallback()
    }

    if (controller.activeTarget == target) {
        ListeningIndicator(
            level = controller.level,
            onClick = controller::finish,
        )
    } else {
        IconButton(
            onClick = {
                onDictationStarted()
                when {
                    !controller.isAvailable -> launchFallback()
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED -> startInApp()

                    else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = stringResource(R.string.modal_content_voice_input_cd),
            )
        }
    }
}

/** Stop button with a halo that breathes gently and swells with your voice. */
@Composable
private fun ListeningIndicator(
    level: Float,
    onClick: () -> Unit,
) {
    val animatedLevel by animateFloatAsState(
        targetValue = level,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dictationLevel",
    )
    val breathing by rememberInfiniteTransition(label = "dictationBreathing").animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
        label = "dictationBreathingScale",
    )
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(
                onClickLabel = stringResource(R.string.modal_content_voice_input_stop_cd),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    val scale = breathing + animatedLevel * 0.45f
                    scaleX = scale
                    scaleY = scale
                }
                .background(primary.copy(alpha = 0.25f), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = stringResource(R.string.modal_content_voice_input_stop_cd),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun showDictationError(context: Context, error: Int) {
    val message = when (error) {
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER,
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> R.string.modal_content_voice_input_error_network

        else -> R.string.modal_content_voice_input_error_generic
    }
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
