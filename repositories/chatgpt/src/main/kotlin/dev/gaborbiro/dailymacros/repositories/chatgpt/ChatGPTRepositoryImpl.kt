package dev.gaborbiro.dailymacros.repositories.chatgpt

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysis
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.OngoingInsightsRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.WeeklyInsightsRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_ANALYSIS_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_ANALYSIS_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_INSIGHTS_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_INSIGHTS_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_ONGOING_INSIGHTS_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_ONGOING_INSIGHTS_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_RECOGNITION_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_RECOGNITION_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_MODEL
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_REASONING_EFFORT
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_INSIGHTS_MODEL
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_INSIGHTS_REASONING_EFFORT
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_INSIGHTS_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_INSIGHTS_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ONGOING_INSIGHTS_MODEL
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ONGOING_INSIGHTS_REASONING_EFFORT
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ONGOING_INSIGHTS_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ONGOING_INSIGHTS_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_MODEL
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_REASONING_EFFORT
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.foodPhotoRecognitionModel
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.foodPhotoRecognitionReasoningEffort
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.nutrientAnalysisModel
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.nutrientAnalysisReasoningEffort
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.ongoingInsightsModel
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.ongoingInsightsReasoningEffort
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.weeklyInsightsModel
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.weeklyInsightsReasoningEffort
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toApiModel
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toFoodRecognitionResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toNutrientAnalysisResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toOngoingInsightsResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toWeeklyInsightsResponse
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
        PromptSegment.Editable(
            id = SEG_RECOGNITION_MODEL,
            label = "Model",
            defaultText = foodPhotoRecognitionModel,
            singleLine = true,
        ),
        PromptSegment.Editable(
            id = SEG_RECOGNITION_REASONING_EFFORT,
            label = "Reasoning effort",
            defaultText = foodPhotoRecognitionReasoningEffort,
            hint = "none, minimal, low, medium, high, xhigh",
            singleLine = true,
        ),
        PromptSegment.Editable(
            id = SEG_RECOGNITION_SYSTEM,
            label = "System message",
            defaultText = DEFAULT_RECOGNITION_SYSTEM,
        ),
        PromptSegment.Locked("{photos}"),
        PromptSegment.Editable(
            id = SEG_RECOGNITION_USER,
            label = "User message",
            defaultText = DEFAULT_RECOGNITION_USER,
        ),
    )

    override fun getAnalysisPromptSegments(): List<PromptSegment> = listOf(
        PromptSegment.Editable(
            id = SEG_ANALYSIS_MODEL,
            label = "Model",
            defaultText = nutrientAnalysisModel,
            singleLine = true,
        ),
        PromptSegment.Editable(
            id = SEG_ANALYSIS_REASONING_EFFORT,
            label = "Reasoning effort",
            defaultText = nutrientAnalysisReasoningEffort,
            hint = "none, minimal, low, medium, high, xhigh",
            singleLine = true,
        ),
        PromptSegment.Editable(
            id = SEG_ANALYSIS_SYSTEM,
            label = "System message",
            defaultText = DEFAULT_ANALYSIS_SYSTEM,
        ),
        PromptSegment.Locked("{photos}"),
        PromptSegment.Editable(
            id = SEG_ANALYSIS_USER,
            label = "User message",
            defaultText = DEFAULT_ANALYSIS_USER,
            hint = "Use {title} and {description} as placeholders for the meal title and description.",
        ),
    )

    override suspend fun getWeeklyInsights(request: WeeklyInsightsRequest): Map<String, String> {
        return mappingApiErrors {
            runCatching(logTag = "getWeeklyInsights") {
                val response = service.callResponses(request = request.toApiModel())
                return@runCatching parse(response).toWeeklyInsightsResponse()
            }
        }
    }

    override fun getInsightsPromptSegments(): List<PromptSegment> = listOf(
        PromptSegment.Editable(
            id = SEG_INSIGHTS_MODEL,
            label = "Model",
            defaultText = weeklyInsightsModel,
            singleLine = true,
        ),
        PromptSegment.Editable(
            id = SEG_INSIGHTS_REASONING_EFFORT,
            label = "Reasoning effort",
            defaultText = weeklyInsightsReasoningEffort,
            hint = "none, minimal, low, medium, high, xhigh",
            singleLine = true,
        ),
        PromptSegment.Editable(
            id = SEG_INSIGHTS_SYSTEM,
            label = "System message",
            defaultText = DEFAULT_INSIGHTS_SYSTEM,
        ),
        PromptSegment.Locked("{diary}"),
        PromptSegment.Editable(
            id = SEG_INSIGHTS_USER,
            label = "User message",
            defaultText = DEFAULT_INSIGHTS_USER,
        ),
    )

    override suspend fun getOngoingInsights(request: OngoingInsightsRequest): String {
        return mappingApiErrors {
            runCatching(logTag = "getOngoingInsights") {
                val response = service.callResponses(request = request.toApiModel())
                return@runCatching parse(response).toOngoingInsightsResponse()
            }
        }
    }

    override fun getOngoingInsightsPromptSegments(): List<PromptSegment> = listOf(
        PromptSegment.Editable(
            id = SEG_ONGOING_INSIGHTS_MODEL,
            label = "Model",
            defaultText = ongoingInsightsModel,
            singleLine = true,
        ),
        PromptSegment.Editable(
            id = SEG_ONGOING_INSIGHTS_REASONING_EFFORT,
            label = "Reasoning effort",
            defaultText = ongoingInsightsReasoningEffort,
            hint = "none, minimal, low, medium, high, xhigh",
            singleLine = true,
        ),
        PromptSegment.Editable(
            id = SEG_ONGOING_INSIGHTS_SYSTEM,
            label = "System message",
            defaultText = DEFAULT_ONGOING_INSIGHTS_SYSTEM,
        ),
        PromptSegment.Locked("{diary}"),
        PromptSegment.Editable(
            id = SEG_ONGOING_INSIGHTS_USER,
            label = "User message",
            defaultText = DEFAULT_ONGOING_INSIGHTS_USER,
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
