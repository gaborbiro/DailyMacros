package dev.gaborbiro.dailymacros.features.settings.export

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

fun interface OpenPublicDocumentUseCase {

    suspend fun execute(): Result<Uri>
}

/** User closed the SAF picker without choosing a file. */
class OpenDocumentCancelled : RuntimeException("Open document cancelled by user")

/**
 * UI helper for Storage Access Framework → Open Document.
 *
 * Must be registered on the activity before STARTED.
 * Only one [execute] call may be active at a time.
 */
class OpenPublicDocumentUseCaseImpl(
    activity: ComponentActivity,
) : OpenPublicDocumentUseCase {

    private var continuation: CancellableContinuation<Result<Uri>>? = null

    private val launcher =
        activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
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

    override suspend fun execute(): Result<Uri> = suspendCancellableCoroutine { cont ->
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
