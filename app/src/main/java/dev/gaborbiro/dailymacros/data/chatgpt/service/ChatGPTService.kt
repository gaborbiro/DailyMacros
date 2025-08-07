package dev.gaborbiro.dailymacros.data.chatgpt.service

import dev.gaborbiro.dailymacros.data.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.ChatGPTRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


internal interface ChatGPTService {

    @Headers("Content-Type: application/json")
    @POST("v1/responses")
    suspend fun callResponses(
        @Body request: ChatGPTRequest,
    ): Response<ChatGPTResponse>
}
