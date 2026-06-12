package dev.gaborbiro.dailymacros.repositories.chatgpt

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysis
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.ANALYSIS_ACCURACY_RULE_1
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.ANALYSIS_OUTPUT_SCHEMA
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_ANALYSIS_CONFLICT_RESOLUTION
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_ANALYSIS_CONTRIBUTOR_HINT
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_ANALYSIS_INTRO
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_ANALYSIS_PACKAGING_RULE
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_ANALYSIS_PRINCIPLES
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_RECOGNITION_APPROACH
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_RECOGNITION_INTRO
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.DEFAULT_RECOGNITION_TASK
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.RECOGNITION_OUTPUT_FORMAT
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_CONFLICT_RESOLUTION
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_CONTRIBUTOR_HINT
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_EXTRA
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_INTRO
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_PACKAGING_RULE
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_PRINCIPLES
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_ANALYSIS_USER_EXTRA
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_APPROACH
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_EXTRA
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_INTRO
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_TASK
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.SEG_RECOGNITION_USER_EXTRA
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
            id = SEG_RECOGNITION_INTRO,
            label = "Role",
            defaultText = DEFAULT_RECOGNITION_INTRO,
        ),
        PromptSegment.Locked("The user provides one or more photos of a meal, drink, or food item.\n"),
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
            id = SEG_RECOGNITION_EXTRA,
            label = "Extra system rules (optional)",
            defaultText = "",
            hint = "More rules (optional)",
        ),
        PromptSegment.Locked("\n— photos —\n"),
        PromptSegment.Locked("\n— user message —\nTASK: RECOGNITION\n"),
        PromptSegment.Editable(
            id = SEG_RECOGNITION_TASK,
            label = "Task instruction",
            defaultText = DEFAULT_RECOGNITION_TASK,
        ),
        PromptSegment.Editable(
            id = SEG_RECOGNITION_USER_EXTRA,
            label = "Extra user rules (optional)",
            defaultText = "",
            hint = "More rules (optional)",
        ),
        PromptSegment.Locked("\n\n$RECOGNITION_OUTPUT_FORMAT"),
    )

    override fun getAnalysisPromptSegments(): List<PromptSegment> = listOf(
        PromptSegment.Locked("— system message —\n"),
        PromptSegment.Editable(
            id = SEG_ANALYSIS_INTRO,
            label = "Role",
            defaultText = DEFAULT_ANALYSIS_INTRO,
        ),
        PromptSegment.Locked(
            "\nThe user may provide:\n" +
            "• Photos of a meal or drink.\n" +
            "• A title and/or description.\n"
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
            "- Only return valid JSON.\n"
        ),
        PromptSegment.Locked("\n$ANALYSIS_ACCURACY_RULE_1\n"),
        PromptSegment.Editable(
            id = SEG_ANALYSIS_PACKAGING_RULE,
            label = "Packaging accuracy rules",
            defaultText = DEFAULT_ANALYSIS_PACKAGING_RULE,
            hint = "You can add more numbered rules below (e.g. \"3. …\"). Keep numbering sequential.",
        ),
        PromptSegment.Editable(
            id = SEG_ANALYSIS_EXTRA,
            label = "Extra system rules (optional)",
            defaultText = "",
            hint = "More rules (optional)",
        ),
        PromptSegment.Locked(
            "\nLANGUAGE RULES (locked):\n" +
            "- All output MUST be in English.\n"
        ),
        PromptSegment.Locked("\n— photos —\n"),
        PromptSegment.Locked("\n— user message —\nTASK: NUTRIENT_ESTIMATION\n"),
        PromptSegment.Editable(
            id = SEG_ANALYSIS_CONFLICT_RESOLUTION,
            label = "Input priority",
            defaultText = DEFAULT_ANALYSIS_CONFLICT_RESOLUTION,
        ),
        PromptSegment.Locked(
            "\nTitle: [meal title]\nDescription: [meal description]\n\n" + ANALYSIS_OUTPUT_SCHEMA + "\n"
        ),
        PromptSegment.Locked("\ntopContributorIngredients RULES:\n"),
        PromptSegment.Editable(
            id = SEG_ANALYSIS_CONTRIBUTOR_HINT,
            label = "Contributor ingredients hint",
            defaultText = DEFAULT_ANALYSIS_CONTRIBUTOR_HINT,
        ),
        PromptSegment.Editable(
            id = SEG_ANALYSIS_USER_EXTRA,
            label = "Extra user rules (optional)",
            defaultText = "",
            hint = "More rules (optional)",
        ),
        PromptSegment.Locked(
            "\nIf estimation is not possible:\n" +
            "{\"error\": \"<one short, specific sentence explaining what is missing or unclear>\"}"
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
