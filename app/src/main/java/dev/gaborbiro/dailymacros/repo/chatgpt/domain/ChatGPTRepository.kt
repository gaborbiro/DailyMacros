package dev.gaborbiro.dailymacros.repo.chatgpt.domain

import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.FoodRecognitionResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientAnalysisResponse

interface ChatGPTRepository {

    suspend fun recogniseFood(request: FoodRecognitionRequest): FoodRecognitionResponse

    suspend fun analyseNutrients(request: NutrientAnalysisRequest): NutrientAnalysisResponse
}
