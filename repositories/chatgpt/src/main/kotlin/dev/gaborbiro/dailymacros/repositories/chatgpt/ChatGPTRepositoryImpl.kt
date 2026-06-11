package dev.gaborbiro.dailymacros.repositories.chatgpt

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysis
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_ANALYSIS_PRINCIPLES
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_RECOGNITION_APPROACH
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_CONTEXT
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_PRINCIPLES
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_APPROACH
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_CONTEXT
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
        PromptSegment.Locked(
            "You are a food identifier for a macronutrient tracker app.\n" +
            "The user provides one or more photos of a meal, drink, or food item.\n"
        ),
        PromptSegment.Editable(
            id = SEG_RECOGNITION_APPROACH,
            label = "Identification instruction",
            defaultText = DEFAULT_RECOGNITION_APPROACH,
        ),
        PromptSegment.Locked(
            "\nLANGUAGE RULES:\n" +
            "- All output MUST be in English.\n" +
            "- If packaging is not in English, translate to English before returning output.\n"
        ),
        PromptSegment.Editable(
            id = SEG_RECOGNITION_CONTEXT,
            label = "Dietary context (optional)",
            defaultText = "",
            hint = "E.g. \"I mostly eat Portuguese home cooking\" or \"I often photograph restaurant meals\"",
        ),
    )

    override fun getAnalysisPromptSegments(): List<PromptSegment> = listOf(
        PromptSegment.Locked(
            "You are a nutritional analyst for a macronutrient tracker app.\n" +
            "The user may provide photos of a meal and/or a title and description.\n"
        ),
        PromptSegment.Editable(
            id = SEG_ANALYSIS_PRINCIPLES,
            label = "Estimation principles",
            defaultText = DEFAULT_ANALYSIS_PRINCIPLES,
        ),
        PromptSegment.Locked(
            "\nStructural rules (locked):\n" +
            "- Round numbers to 1 decimal place, except salt (2 decimal places).\n" +
            "- If total fat is estimated, also estimate ofWhichSaturated.\n" +
            "- If carbohydrate is estimated, also estimate ofWhichSugar and ofWhichAddedSugar.\n" +
            "- ofWhichAddedSugar must never exceed ofWhichSugar.\n" +
            "- If vegetables, grains, legumes or seeds are present, estimate fibre.\n" +
            "- Only return valid JSON.\n" +
            "\nACCURACY RULES (locked):\n" +
            "1. For nutritional values clearly visible on packaging, extract and use those exact values.\n" +
            "2. Estimate any missing values — never default missing values to 0.\n" +
            "\nLANGUAGE RULES (locked):\n" +
            "- All output MUST be in English.\n"
        ),
        PromptSegment.Editable(
            id = SEG_ANALYSIS_CONTEXT,
            label = "Dietary context (optional)",
            defaultText = "",
            hint = "E.g. \"I mostly eat Portuguese home cooking\" or \"I track a high-protein diet\"",
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
