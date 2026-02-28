package dev.gaborbiro.dailymacros.features.settings.export

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

interface CreatePublicDocumentUseCase {

    suspend fun execute(suggestedFileName: String): Uri?
}

/**
 * UI utility for launching Storage Access Framework > Create Document.
 *
 * Must be called from the main thread.
 * Only one execute() call may be active at a time.
 */
class CreatePublicDocumentUseCaseImpl(activity: ComponentActivity) : CreatePublicDocumentUseCase {

    private var continuation: CancellableContinuation<Uri?>? = null

    private val launcher =
        activity.registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            continuation?.resume(uri) { _, _, _ -> }
            continuation = null
        }

    override suspend fun execute(
        suggestedFileName: String,
    ): Uri? = suspendCancellableCoroutine { cont ->
        check(continuation == null) {
            "CreateJsonDocumentUseCase.execute() called while another request is active"
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
