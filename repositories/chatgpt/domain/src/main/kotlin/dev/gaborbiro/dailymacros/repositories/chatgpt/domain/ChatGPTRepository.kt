package dev.gaborbiro.dailymacros.repositories.chatgpt.domain

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodTitle
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysis
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.OngoingWeekInsightsRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.OngoingWeekInsights
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.WeeklyInsightsRequest

/**
 * All operations translate API/transport failures into [dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.ChatGPTDomainError]
 * before throwing. No service-layer exception (e.g. `ChatGPTApiError`) leaks out of the repository.
 */
interface ChatGPTRepository {

    /** @throws dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.ChatGPTDomainError on any API/transport failure. */
    suspend fun recogniseFood(request: FoodRecognitionRequest): FoodTitle

    /** @throws dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.ChatGPTDomainError on any API/transport failure. */
    suspend fun analyseNutrients(request: NutrientAnalysisRequest): NutrientAnalysis

    fun getRecognitionPromptSegments(): List<PromptSegment>

    fun getAnalysisPromptSegments(): List<PromptSegment>

    fun getInsightsPromptSegments(): List<PromptSegment>

    fun getOngoingInsightsPromptSegments(): List<PromptSegment>

    /** @throws dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.ChatGPTDomainError on any API/transport failure. */
    suspend fun getWeeklyInsights(request: WeeklyInsightsRequest): Map<String, String>

    /** @throws dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.ChatGPTDomainError on any API/transport failure. */
    suspend fun getOngoingInsights(request: OngoingWeekInsightsRequest): OngoingWeekInsights
}
