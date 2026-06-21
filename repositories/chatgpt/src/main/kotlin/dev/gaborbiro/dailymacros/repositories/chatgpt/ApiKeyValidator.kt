package dev.gaborbiro.dailymacros.repositories.chatgpt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyValidator @Inject constructor() {

    private val client = OkHttpClient()

    suspend fun validate(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.openai.com/v1/models")
            .header("Authorization", "Bearer $apiKey")
            .build()
        runCatching { client.newCall(request).execute().use { it.code == 200 } }.getOrDefault(false)
    }
}
