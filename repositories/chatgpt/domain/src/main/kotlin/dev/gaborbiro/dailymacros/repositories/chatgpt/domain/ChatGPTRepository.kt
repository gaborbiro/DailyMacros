package dev.gaborbiro.dailymacros.repositories.chatgpt.domain

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysis

interface ChatGPTRepository {

    suspend fun recogniseFood(request: FoodRecognitionRequest): FoodRecognitionResult

    suspend fun analyseNutrients(request: NutrientAnalysisRequest): NutrientAnalysis
}
