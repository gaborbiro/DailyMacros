package dev.gaborbiro.nutrition.data.chatgpt.service

import dev.gaborbiro.nutrition.data.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.nutrition.data.chatgpt.service.model.ChatGPTResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


internal interface ChatGPTService {

    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun getChatCompletions(
        @Body request: ChatGPTRequest,
    ): Response<ChatGPTResponse>
}