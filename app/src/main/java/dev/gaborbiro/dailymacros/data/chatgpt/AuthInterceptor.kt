package dev.gaborbiro.dailymacros.data.chatgpt

import dev.gaborbiro.dailymacros.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response


class AuthInterceptor : Interceptor {

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()
        builder.header(HEADER_AUTHORIZATION, "Bearer ${BuildConfig.CHATGPT_API_KEY}")
        return chain.proceed(builder.build())
    }
}
