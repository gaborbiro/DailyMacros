package dev.gaborbiro.dailymacros.repositories.chatgpt

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Decides how each OpenAI-bound request is authenticated and routed.
 *
 * - If the user has set a personal API key (the hidden "Personalise AI"
 *   feature), the request goes straight to OpenAI with that key, bypassing the
 *   proxy and its spending caps. This is the dev / power-user path.
 * - Otherwise the request is routed to the [PROXY_URL] Cloud Function and
 *   authenticated with a Firebase ID token (anonymous auth). The OpenAI key no
 *   longer ships in the app; the proxy holds it and enforces the caps.
 */
class AuthInterceptor(
    private val settingsRepository: SettingsRepository,
    private val firebaseAuth: FirebaseAuth,
) : Interceptor {

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"

        // OpenAI proxy Cloud Function. Update this if the region, project id, or
        // function name change (see functions/README.md).
        private const val PROXY_URL =
            "https://us-central1-dailymacros-9fab8.cloudfunctions.net/openaiProxy"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val override = settingsRepository.getApiKeyOverride()
        val request = if (override != null) {
            chain.request().newBuilder()
                .header(HEADER_AUTHORIZATION, "Bearer $override")
                .build()
        } else {
            val token = firebaseIdToken()
            val proxyUrl = PROXY_URL.toHttpUrl()
            val rewrittenUrl = chain.request().url.newBuilder()
                .scheme(proxyUrl.scheme)
                .host(proxyUrl.host)
                .port(proxyUrl.port)
                .encodedPath(proxyUrl.encodedPath)
                .build()
            chain.request().newBuilder()
                .url(rewrittenUrl)
                .header(HEADER_AUTHORIZATION, "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }

    /**
     * Returns a Firebase ID token, signing in anonymously first if needed.
     * Runs on OkHttp's worker thread, so blocking on the Tasks is fine. Any
     * failure is surfaced as an [IOException] so the repository maps it to the
     * usual "no internet / try again" error rather than crashing.
     */
    private fun firebaseIdToken(): String {
        return try {
            val user = firebaseAuth.currentUser
                ?: Tasks.await(firebaseAuth.signInAnonymously()).user
                ?: throw IOException("Anonymous sign-in returned no user")
            Tasks.await(user.getIdToken(false)).token
                ?: throw IOException("Firebase returned a null ID token")
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Firebase authentication failed", e)
        }
    }
}
