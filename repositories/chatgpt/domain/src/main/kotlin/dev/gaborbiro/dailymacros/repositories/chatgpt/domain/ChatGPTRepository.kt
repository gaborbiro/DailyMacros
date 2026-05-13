package dev.gaborbiro.dailymacros.repositories.chatgpt.domain

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysis
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.VariabilityMiningResult

/**
 * All operations translate API/transport failures into [dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.ChatGPTDomainError]
 * before throwing. No service-layer exception (e.g. `ChatGPTApiError`) leaks out of the repository.
 */
interface ChatGPTRepository {

    /** @throws dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.ChatGPTDomainError on any API/transport failure. */
    suspend fun recogniseFood(request: FoodRecognitionRequest): FoodRecognitionResult

    /** @throws dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.ChatGPTDomainError on any API/transport failure. */
    suspend fun analyseNutrients(request: NutrientAnalysisRequest): NutrientAnalysis

    /**
     * [userMessageJson] is the full user payload (e.g. meal_observations envelope) as a JSON string.
     *
     * @throws dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.ChatGPTDomainError on any API/transport failure.
     */
    suspend fun mineMealVariability(userMessageJson: String): VariabilityMiningResult
}
