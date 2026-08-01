package dev.gaborbiro.dailymacros.repositories.billing

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Posts a purchase token to the `verifySubscription` Cloud Function so the
 * server can independently confirm it with Play and gate `openaiProxy` on
 * the result. One-shot (not periodic, unlike NutrientAnalysisWorker) — once
 * the server confirms a token there's nothing left to retry for it.
 */
@HiltWorker
class VerifyPurchaseWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted private val workerParameters: WorkerParameters,
    private val firebaseAuth: FirebaseAuth,
) : CoroutineWorker(appContext, workerParameters) {

    companion object {
        private const val ARGS_PURCHASE_TOKEN = "purchase_token"
        private const val ARGS_PRODUCT_ID = "product_id"

        // Cloud Function endpoint (functions/subscriptions.js). Update if the
        // region/project id/function name change (see functions/README.md).
        private const val VERIFY_URL =
            "https://us-central1-dailymacros-9fab8.cloudfunctions.net/verifySubscription"

        fun enqueue(context: Context, purchaseToken: String, productId: String) {
            val workRequest = OneTimeWorkRequestBuilder<VerifyPurchaseWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    Data.Builder()
                        .putString(ARGS_PURCHASE_TOKEN, purchaseToken)
                        .putString(ARGS_PRODUCT_ID, productId)
                        .build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "verify_purchase_${purchaseToken.hashCode()}",
                    ExistingWorkPolicy.KEEP,
                    workRequest,
                )
        }
    }

    private val client by lazy { OkHttpClient() }

    override suspend fun doWork(): Result {
        val purchaseToken = workerParameters.inputData.getString(ARGS_PURCHASE_TOKEN)
            ?: return Result.failure()
        val productId = workerParameters.inputData.getString(ARGS_PRODUCT_ID)
            ?: return Result.failure()

        return try {
            val idToken = firebaseIdToken()
            val body = JSONObject()
                .put("purchaseToken", purchaseToken)
                .put("productId", productId)
                .toString()
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(VERIFY_URL)
                .header("Authorization", "Bearer $idToken")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    BillingPrefs(applicationContext).lastVerifiedPurchaseToken = purchaseToken
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /** Mirrors AuthInterceptor.firebaseIdToken() (repositories/chatgpt) — duplicated rather
     * than shared, to keep repositories:billing decoupled from repositories:chatgpt. */
    private fun firebaseIdToken(): String {
        val user = firebaseAuth.currentUser
            ?: Tasks.await(firebaseAuth.signInAnonymously()).user
            ?: throw IOException("Anonymous sign-in returned no user")
        return Tasks.await(user.getIdToken(false)).token
            ?: throw IOException("Firebase returned a null ID token")
    }
}
