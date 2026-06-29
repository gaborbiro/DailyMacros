package dev.gaborbiro.dailymacros.repositories.chatgpt

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.OngoingWeekInsightsRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.OngoingWeekInsightsResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.WeeklyInsightsRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.WeeklyInsightsResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_ANALYSIS_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_ANALYSIS_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_WEEKLY_INSIGHTS_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_WEEKLY_INSIGHTS_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_ONGOING_WEEK_INSIGHTS_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_ONGOING_WEEK_INSIGHTS_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_RECOGNITION_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_RECOGNITION_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_MODEL
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_REASONING_EFFORT
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_WEEKLY_INSIGHTS_MODEL
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_WEEKLY_INSIGHTS_REASONING_EFFORT
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_WEEKLY_INSIGHTS_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_WEEKLY_INSIGHTS_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ONGOING_WEEK_INSIGHTS_MODEL
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ONGOING_WEEK_INSIGHTS_REASONING_EFFORT
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ONGOING_WEEK_INSIGHTS_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ONGOING_WEEK_INSIGHTS_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_MODEL
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_REASONING_EFFORT
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_SYSTEM
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_USER
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.foodPhotoRecognitionModel
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.foodPhotoRecognitionReasoningEffort
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.nutrientAnalysisModel
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.nutrientAnalysisReasoningEffort
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.ongoingWeekInsightsModel
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.ongoingWeekInsightsReasoningEffort
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.weeklyInsightsModel
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.weeklyInsightsReasoningEffort
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toApiModel
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toFoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toNutrientAnalysisResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toOngoingInsightsResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.toWeeklyInsightsResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.ChatGPTService
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.dailymacros.repositories.chatgpt.utils.parse
import dev.gaborbiro.dailymacros.repositories.chatgpt.utils.runCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request


internal class ChatGPTRepositoryImpl(
    private val service: ChatGPTService,
    private val mapper: ChatGPTMapper,
) : ChatGPTRepository {

    private val validationClient by lazy { OkHttpClient() }

    override suspend fun recogniseFood(request: FoodRecognitionRequest): FoodRecognitionResult {
        return mappingApiErrors {
            runCatching(logTag = "recogniseFood") {
                val response = service.callResponses(
                    request = request.toApiModel(),
                )
                return@runCatching parse(response)
                    .toFoodRecognitionResult()
            }
        }
    }

    override suspend fun analyseNutrients(request: NutrientAnalysisRequest): NutrientAnalysisResult {
        return mappingApiErrors {
            runCatching(logTag = "analyseNutrients") {
                val response = service.callResponses(
                    request = request.toApiModel(),
                )
                val imageCount = request.base64Images.size
                return@runCatching mapper.map(parse(response).toNutrientAnalysisResponse(), imageCount)
            }
        }
    }

    override suspend fun getWeeklyInsights(request: WeeklyInsightsRequest): WeeklyInsightsResult {
        return mappingApiErrors {
            runCatching(logTag = "getWeeklyInsights") {
                val response = service.callResponses(request = request.toApiModel())
                return@runCatching WeeklyInsightsResult(parse(response).toWeeklyInsightsResponse())
            }
        }
    }

    override suspend fun getOngoingInsights(request: OngoingWeekInsightsRequest): OngoingWeekInsightsResult {
        return mappingApiErrors {
            runCatching(logTag = "getOngoingWeekInsights") {
                val response = service.callResponses(request = request.toApiModel())
                return@runCatching parse(response).toOngoingInsightsResult()
            }
        }
    }

    override fun getDefaultFoodRecognitionPromptSegments(): List<PromptSegment> = listOf(
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

    override fun getDefaultNutrientAnalysisPromptSegments(): List<PromptSegment> = listOf(
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

    override fun getDefaultWeeklyInsightsPromptSegments(): List<PromptSegment> = listOf(
        PromptSegment.Editable(
            id = SEG_WEEKLY_INSIGHTS_MODEL,
            label = "Model",
            defaultText = weeklyInsightsModel,
            singleLine = true,
        ),
        PromptSegment.Editable(
            id = SEG_WEEKLY_INSIGHTS_REASONING_EFFORT,
            label = "Reasoning effort",
            defaultText = weeklyInsightsReasoningEffort,
            hint = "none, minimal, low, medium, high, xhigh",
            singleLine = true,
        ),
        PromptSegment.Editable(
            id = SEG_WEEKLY_INSIGHTS_SYSTEM,
            label = "System message",
            defaultText = DEFAULT_WEEKLY_INSIGHTS_SYSTEM,
        ),
        PromptSegment.Locked("{diary}"),
        PromptSegment.Editable(
            id = SEG_WEEKLY_INSIGHTS_USER,
            label = "User message",
            defaultText = DEFAULT_WEEKLY_INSIGHTS_USER,
        ),
    )

    override fun getDefaultOngoingWeekInsightsPromptSegments(): List<PromptSegment> = listOf(
        PromptSegment.Editable(
            id = SEG_ONGOING_WEEK_INSIGHTS_MODEL,
            label = "Model",
            defaultText = ongoingWeekInsightsModel,
            singleLine = true,
        ),
        PromptSegment.Editable(
            id = SEG_ONGOING_WEEK_INSIGHTS_REASONING_EFFORT,
            label = "Reasoning effort",
            defaultText = ongoingWeekInsightsReasoningEffort,
            hint = "none, minimal, low, medium, high, xhigh",
            singleLine = true,
        ),
        PromptSegment.Editable(
            id = SEG_ONGOING_WEEK_INSIGHTS_SYSTEM,
            label = "System message",
            defaultText = DEFAULT_ONGOING_WEEK_INSIGHTS_SYSTEM,
        ),
        PromptSegment.Locked("{diary}"),
        PromptSegment.Editable(
            id = SEG_ONGOING_WEEK_INSIGHTS_USER,
            label = "User message",
            defaultText = DEFAULT_ONGOING_WEEK_INSIGHTS_USER,
        ),
    )

    override suspend fun validateApiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.openai.com/v1/models")
            .header("Authorization", "Bearer $apiKey")
            .build()
        runCatching { validationClient.newCall(request).execute().use { it.code == 200 } }.getOrDefault(false)
    }

    private inline fun <T> mappingApiErrors(block: () -> T): T {
        return try {
            block()
        } catch (apiError: ChatGPTApiError) {
            throw mapper.map(apiError)
        }
    }
}
