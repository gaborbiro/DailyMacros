package dev.gaborbiro.dailymacros.repositories.chatgpt.domain

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.OngoingWeekInsightsRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.OngoingWeekInsightsResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.WeeklyInsightsRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.WeeklyInsightsResult

/**
 * All operations translate API/transport failures into [dev.gaborbiro.dailymacros.repositories.common.model.DomainError]
 * before throwing. No service-layer exception (e.g. `AiRequestError`) leaks out of the repository.
 */
interface ChatGPTRepository {

    /** @throws dev.gaborbiro.dailymacros.repositories.common.model.DomainError on any API/transport failure. */
    suspend fun recogniseFood(request: FoodRecognitionRequest): FoodRecognitionResult

    /** @throws dev.gaborbiro.dailymacros.repositories.common.model.DomainError on any API/transport failure. */
    suspend fun analyseNutrients(request: NutrientAnalysisRequest): NutrientAnalysisResult

    /** @throws dev.gaborbiro.dailymacros.repositories.common.model.DomainError on any API/transport failure. */
    suspend fun getWeeklyInsights(request: WeeklyInsightsRequest): WeeklyInsightsResult

    /** @throws dev.gaborbiro.dailymacros.repositories.common.model.DomainError on any API/transport failure. */
    suspend fun getOngoingInsights(request: OngoingWeekInsightsRequest): OngoingWeekInsightsResult

    fun getDefaultFoodRecognitionPromptSegments(): List<PromptSegment>

    fun getDefaultNutrientAnalysisPromptSegments(): List<PromptSegment>

    fun getDefaultWeeklyInsightsPromptSegments(): List<PromptSegment>

    fun getDefaultOngoingWeekInsightsPromptSegments(): List<PromptSegment>

    /** Returns true if the given API key is accepted by the OpenAI API. Never throws. */
    suspend fun validateApiKey(apiKey: String): Boolean
}
