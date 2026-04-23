package dev.gaborbiro.dailymacros.repositories.chatgpt

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.VariabilityMiningResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.extractAssistantJsonText
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toApiModel
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toFoodRecognitionResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toNutrientAnalysisResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.variabilityMiningRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.ChatGPTService
import dev.gaborbiro.dailymacros.repositories.chatgpt.util.parse
import dev.gaborbiro.dailymacros.repositories.chatgpt.util.runCatching


class ChatGPTRepositoryImpl(
    private val service: ChatGPTService,
) : ChatGPTRepository {

    override suspend fun recogniseFood(request: FoodRecognitionRequest): FoodRecognitionResult {
        return runCatching(logTag = "recogniseFood") {
            val response = service.callResponses(
                request = request.toApiModel(),
            )
            return@runCatching parse(response)
                .toFoodRecognitionResponse()
        }
    }

    override suspend fun analyseNutrients(request: NutrientAnalysisRequest): NutrientAnalysisResult {
        return runCatching(logTag = "analyseNutrients") {
            val response = service.callResponses(
                request = request.toApiModel(),
            )
            return@runCatching parse(response)
                .toNutrientAnalysisResponse()
        }
    }

    override suspend fun mineMealVariability(userMessageJson: String): VariabilityMiningResult {
        return runCatching(logTag = "mineMealVariability") {
            val response = service.callResponses(
                request = variabilityMiningRequest(userMessageJson),
            )
            val body = parse(response)
            VariabilityMiningResult(profileJson = body.extractAssistantJsonText())
        }
    }
}
