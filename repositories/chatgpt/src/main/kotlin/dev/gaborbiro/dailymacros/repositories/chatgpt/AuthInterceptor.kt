package dev.gaborbiro.dailymacros.repositories.chatgpt

import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val settingsRepository: SettingsRepository,
) : Interceptor {

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = settingsRepository.getApiKeyOverride() ?: BuildConfig.CHATGPT_API_KEY
        val request = chain.request().newBuilder()
            .header(HEADER_AUTHORIZATION, "Bearer $apiKey")
            .build()
        return chain.proceed(request)
    }
}
