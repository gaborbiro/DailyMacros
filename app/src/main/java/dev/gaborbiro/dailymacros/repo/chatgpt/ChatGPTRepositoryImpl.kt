package dev.gaborbiro.dailymacros.repo.chatgpt

import dev.gaborbiro.dailymacros.repo.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.FoodRecognitionResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientAnalysisResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.prompts.food.toApiModel
import dev.gaborbiro.dailymacros.repo.chatgpt.prompts.food.toFoodRecognitionResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.prompts.food.toNutrientAnalysisResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.service.ChatGPTService
import dev.gaborbiro.dailymacros.repo.chatgpt.util.parse
import dev.gaborbiro.dailymacros.repo.chatgpt.util.runCatching


internal class ChatGPTRepositoryImpl(
    private val service: ChatGPTService,
) : ChatGPTRepository {

    override suspend fun recogniseFood(request: FoodRecognitionRequest): FoodRecognitionResponse {
        return runCatching(logTag = "recogniseFood") {
            val response = service.callResponses(
                request = request.toApiModel(),
            )
            return@runCatching parse(response)
                .toFoodRecognitionResponse()
        }
    }

    override suspend fun analyseNutrients(request: NutrientAnalysisRequest): NutrientAnalysisResponse {
        return runCatching(logTag = "analyseNutrients") {
            val response = service.callResponses(
                request = request.toApiModel(),
            )
            return@runCatching parse(response)
                .toNutrientAnalysisResponse()
        }
    }
}
