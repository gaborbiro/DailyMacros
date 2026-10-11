package dev.gaborbiro.dailymacros.features.modal.views.dictation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

internal enum class DictationTarget { Title, Description }

/**
 * In-app dictation on top of the device's speech recogniser ([SpeechRecognizer], Google's on most
 * phones - the same engine as Gboard voice typing). Recognised words are written into the target
 * field as they arrive.
 *
 * The recogniser ends its session after a short silence and ignores requests to wait longer, so a
 * session that ends on its own is immediately restarted. Dictation only stops when [finish] (stop
 * button) or [cancel] (typing, screen closed) is called, or on a real error.
 *
 * Only one field dictates at a time; starting another target cancels the current one. All calls
 * must be made on the main thread (SpeechRecognizer requires it).
 */
@Stable
internal class DictationController(private val context: Context) {

    /** The field currently listening, or null. Drives the button's listening state. */
    var activeTarget by mutableStateOf<DictationTarget?>(null)
        private set

    /** Voice loudness, 0..1, for the listening animation. */
    var level by mutableFloatStateOf(0f)
        private set

    // runCatching: Compose previews have no package manager to ask.
    val isAvailable: Boolean = runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var session: Session? = null

    private class Session(
        val target: DictationTarget,
        val base: String,
        val onText: (TextFieldValue) -> Unit,
        val onError: (Int) -> Unit,
    ) {
        val segments = mutableListOf<String>()
        var partial = ""
        var finishing = false
        var consecutiveFailures = 0
    }

    private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

    /** True while [target]'s field may still receive text, including the final words after [finish]. */
    fun isWritingTo(target: DictationTarget): Boolean = session?.target == target

    fun start(
        target: DictationTarget,
        current: TextFieldValue,
        onText: (TextFieldValue) -> Unit,
        onError: (Int) -> Unit,
    ) {
        cancel()
        val s = Session(target, current.text, onText, onError)
        session = s
        activeTarget = target
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(Listener(s))
            startListening(recognizerIntent)
        }
    }

    /** Stop button: stop listening but let the recogniser deliver the last words. */
    fun finish() {
        val s = session ?: return
        s.finishing = true
        activeTarget = null
        level = 0f
        recognizer?.stopListening()
        // Some recognisers never report back after stopListening; don't hold the mic forever.
        handler.postDelayed({ if (session === s) release() }, FINISH_TIMEOUT_MILLIS)
    }

    /** Typing or leaving the screen: drop anything pending. What's already in the field stays. */
    fun cancel() {
        recognizer?.cancel()
        release()
    }

    private fun release() {
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
        session = null
        activeTarget = null
        level = 0f
    }

    private fun restart(s: Session, delayMillis: Long = 0L) {
        val restartNow = {
            if (session === s) recognizer?.startListening(recognizerIntent)
        }
        if (delayMillis > 0) handler.postDelayed(restartNow, delayMillis) else restartNow()
    }

    private fun Session.commitPartial() {
        if (partial.isNotBlank()) segments += partial
        partial = ""
    }

    private fun Session.emit() {
        onText(appendSpokenText(base, segments + partial))
    }

    private inner class Listener(private val s: Session) : RecognitionListener {
        private val isCurrent get() = session === s

        override fun onRmsChanged(rmsdB: Float) {
            if (!isCurrent || s.finishing) return
            // Typical range is about -2..10 dB.
            level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!isCurrent) return
            val text = partialResults.firstResult() ?: return
            s.consecutiveFailures = 0
            s.partial = text
            s.emit()
        }

        override fun onResults(results: Bundle?) {
            if (!isCurrent) return
            s.consecutiveFailures = 0
            results.firstResult()?.let { s.partial = it }
            s.commitPartial()
            s.emit()
            if (s.finishing) release() else restart(s)
        }

        override fun onError(error: Int) {
            if (!isCurrent) return
            s.commitPartial()
            s.emit()
            if (s.finishing) {
                release()
                return
            }
            when (error) {
                // Silence or nothing intelligible: the normal end of a session, keep listening.
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                SpeechRecognizer.ERROR_CLIENT -> {
                    s.consecutiveFailures++
                    if (s.consecutiveFailures > MAX_CONSECUTIVE_FAILURES) {
                        release()
                    } else {
                        restart(s, delayMillis = if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) 0L else RETRY_DELAY_MILLIS)
                    }
                }

                else -> {
                    release()
                    s.onError(error)
                }
            }
        }

        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun Bundle?.firstResult(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }

    private companion object {
        const val FINISH_TIMEOUT_MILLIS = 3_000L
        const val RETRY_DELAY_MILLIS = 300L

        // Silent restarts in a row before giving up, so a recogniser stuck failing doesn't loop
        // forever. Silence timeouts count too: ~10 of them is a good minute of nobody speaking.
        const val MAX_CONSECUTIVE_FAILURES = 10
    }
}
