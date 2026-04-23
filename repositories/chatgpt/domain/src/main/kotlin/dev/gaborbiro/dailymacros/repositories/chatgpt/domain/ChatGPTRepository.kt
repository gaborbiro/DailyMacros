package dev.gaborbiro.dailymacros.repositories.chatgpt.domain

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.VariabilityMiningResult

interface ChatGPTRepository {

    suspend fun recogniseFood(request: FoodRecognitionRequest): FoodRecognitionResult

    suspend fun analyseNutrients(request: NutrientAnalysisRequest): NutrientAnalysisResult

    /**
     * [userMessageJson] is the full user payload (e.g. meal_observations envelope) as a JSON string.
     */
    suspend fun mineMealVariability(userMessageJson: String): VariabilityMiningResult
}
