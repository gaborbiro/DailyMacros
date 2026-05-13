package dev.gaborbiro.dailymacros.features.settings.export

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

fun interface OpenPublicDocumentUseCase {

    suspend fun execute(): Result<Uri>
}

/** User closed the SAF picker without choosing a file. */
class OpenDocumentCancelled : RuntimeException("Open document cancelled by user")

/**
 * UI helper for Storage Access Framework > Open Document from Compose.
 */
@Composable
fun rememberOpenPublicDocumentUseCase(): OpenPublicDocumentUseCase {
    var continuation by remember { mutableStateOf<CancellableContinuation<Result<Uri>>?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val result =
            if (uri != null) {
                Result.success(uri)
            } else {
                Result.failure(OpenDocumentCancelled())
            }
        continuation?.resume(result) { _, _, _ -> }
        continuation = null
    }

    DisposableEffect(Unit) {
        onDispose {
            continuation?.cancel(OpenDocumentCancelled())
            continuation = null
        }
    }

    return remember(launcher) {
        OpenPublicDocumentUseCase {
            suspendCancellableCoroutine { cont ->
                check(continuation == null) {
                    "OpenPublicDocumentUseCase.execute() called while another request is active"
                }

                continuation = cont

                cont.invokeOnCancellation {
                    if (continuation === cont) {
                        continuation = null
                    }
                }

                launcher.launch(arrayOf("*/*"))
            }
        }
    }
}
