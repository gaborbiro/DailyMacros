package dev.gaborbiro.dailymacros.features.settings.export

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

fun interface CreatePublicDocumentUseCase {

    suspend fun execute(suggestedFileName: String): Uri?
}

/**
 * UI utility for launching Storage Access Framework > Create Document from Compose.
 */
@Composable
fun rememberCreatePublicDocumentUseCase(): CreatePublicDocumentUseCase {
    var continuation by remember { mutableStateOf<CancellableContinuation<Uri?>?>(null) }
    val launcher = rememberLauncherForActivityResult(
        // Covers JSON diary exports and full app backups (.tar).
        contract = ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
        continuation?.resume(uri) { _, _, _ -> }
        continuation = null
    }

    DisposableEffect(Unit) {
        onDispose {
            continuation?.cancel()
            continuation = null
        }
    }

    return remember(launcher) {
        CreatePublicDocumentUseCase { suggestedFileName ->
            suspendCancellableCoroutine { cont ->
                check(continuation == null) {
                    "CreatePublicDocumentUseCase.execute() called while another request is active"
                }

                continuation = cont

                cont.invokeOnCancellation {
                    if (continuation === cont) {
                        continuation = null
                    }
                }

                launcher.launch(suggestedFileName)
            }
        }
    }
}
