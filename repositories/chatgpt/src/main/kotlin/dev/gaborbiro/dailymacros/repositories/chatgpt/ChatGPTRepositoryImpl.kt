package dev.gaborbiro.dailymacros.repositories.chatgpt

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysis
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_ANALYSIS_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_ANALYSIS_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_RECOGNITION_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_RECOGNITION_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toApiModel
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toFoodRecognitionResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toNutrientAnalysisResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.ChatGPTService
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.dailymacros.repositories.chatgpt.util.parse
import dev.gaborbiro.dailymacros.repositories.chatgpt.util.runCatching


class ChatGPTRepositoryImpl(
    private val service: ChatGPTService,
    private val mapper: ChatGPTMapper,
) : ChatGPTRepository {

    override suspend fun recogniseFood(request: FoodRecognitionRequest): FoodRecognitionResult {
        return mappingApiErrors {
            runCatching(logTag = "recogniseFood") {
                val response = service.callResponses(
                    request = request.toApiModel(),
                )
                return@runCatching parse(response)
                    .toFoodRecognitionResponse()
            }
        }
    }

    override suspend fun analyseNutrients(request: NutrientAnalysisRequest): NutrientAnalysis {
        return mappingApiErrors {
            runCatching(logTag = "analyseNutrients") {
                val response = service.callResponses(
                    request = request.toApiModel(),
                )
                return@runCatching parse(response)
                    .toNutrientAnalysisResponse(imageCount = request.base64Images.size)
            }
        }
    }

    override fun getRecognitionPromptSegments(): List<PromptSegment> = listOf(
        PromptSegment.Locked("— system message —\n"),
        PromptSegment.Editable(
            id = SEG_RECOGNITION_SYSTEM,
            label = "System message",
            defaultText = DEFAULT_RECOGNITION_SYSTEM,
        ),
        PromptSegment.Locked("\n— photos —\n\n— user message —\n"),
        PromptSegment.Editable(
            id = SEG_RECOGNITION_USER,
            label = "User message",
            defaultText = DEFAULT_RECOGNITION_USER,
        ),
    )

    override fun getAnalysisPromptSegments(): List<PromptSegment> = listOf(
        PromptSegment.Locked("— system message —\n"),
        PromptSegment.Editable(
            id = SEG_ANALYSIS_SYSTEM,
            label = "System message",
            defaultText = DEFAULT_ANALYSIS_SYSTEM,
        ),
        PromptSegment.Locked("\n— photos —\n\n— user message —\n"),
        PromptSegment.Editable(
            id = SEG_ANALYSIS_USER,
            label = "User message",
            defaultText = DEFAULT_ANALYSIS_USER,
            hint = "Use {title} and {description} as placeholders for the meal title and description.",
        ),
    )

    private inline fun <T> mappingApiErrors(block: () -> T): T {
        return try {
            block()
        } catch (apiError: ChatGPTApiError) {
            throw mapper.map(apiError)
        }
    }
}
