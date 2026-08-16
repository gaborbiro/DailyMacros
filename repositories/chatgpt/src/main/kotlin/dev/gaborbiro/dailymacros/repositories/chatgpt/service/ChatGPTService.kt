package dev.gaborbiro.dailymacros.repositories.chatgpt.service

import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ReportOutcomeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query


internal interface ChatGPTService {

    @Headers("Content-Type: application/json")
    @POST("v1/responses")
    suspend fun callResponses(
        @Header("X-Feature") feature: String,
        @Body request: ChatGPTRequest,
    ): Response<ChatGPTResponse>

    // Same proxy URL (AuthInterceptor always rewrites the host/path to it, see
    // its doc comment) - the "?report=1" query param is how the function tells
    // this apart from a real OpenAI-bound call. Used only to self-report that a
    // pre-subscription call actually produced something useful; see
    // functions/index.js's PRESUB_FEATURES doc comment for the trust model.
    @Headers("Content-Type: application/json")
    @POST("v1/responses")
    suspend fun reportOutcome(
        @Query("report") report: Int = 1,
        @Body body: ReportOutcomeRequest,
    ): Response<Unit>
}
