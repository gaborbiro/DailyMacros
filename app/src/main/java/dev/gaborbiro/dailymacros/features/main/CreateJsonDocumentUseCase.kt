package dev.gaborbiro.dailymacros.features.main

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

class CreateJsonDocumentUseCase(
    activity: ComponentActivity,
) {

    private var continuation:
            CancellableContinuation<Uri?>? = null

    private val launcher =
        activity.registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            continuation?.resume(uri) { _, _, _ -> }
            continuation = null
        }

    suspend fun execute(
        suggestedFileName: String,
    ): Uri? = suspendCancellableCoroutine { cont ->
        continuation = cont

        cont.invokeOnCancellation {
            continuation = null
        }

        launcher.launch(suggestedFileName)
    }
}
