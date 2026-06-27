package dev.gaborbiro.dailymacros.repositories.chatgpt.prompts

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.InputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ReasoningLevel
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.Role


internal val DEFAULT_RECOGNITION_SYSTEM = """
You are a food identifier for a macronutrient tracker app.

The user provides one or more photos of a meal.

LANGUAGE RULES:
- All output (title or error message) MUST be in {phone_language}.
- Never switch output language based on packaging language.
- If packaging text is not in {phone_language}, translate output into {phone_language}.
""".trimIndent()

internal val DEFAULT_RECOGNITION_USER = """
TASK: RECOGNITION
Concisely identify the food shown in the photos.

Output JSON format:
{
  "title": ""
}

If food cannot be identified:
{
  "error": "<one short sentence explaining clearly why food cannot be identified>"
}
""".trimIndent()

internal fun FoodRecognitionRequest.toApiModel() = ChatGPTRequest(
    model = customizations.systemPrompt(SEG_RECOGNITION_MODEL, foodPhotoRecognitionModel),
    reasoning = ReasoningLevel(customizations.systemPrompt(SEG_RECOGNITION_REASONING_EFFORT, foodPhotoRecognitionReasoningEffort)),
    input = listOf(
        ContentEntry(
            role = Role.system,
            content = listOf(InputContent.Text(
                this.customizations.systemPrompt(SEG_RECOGNITION_SYSTEM, DEFAULT_RECOGNITION_SYSTEM)
                    .replace("{phone_language}", this.phoneLanguage)
            )),
        ),
        ContentEntry(
            role = Role.user,
            content = base64Images.map {
                InputContent.Image(it)
            }
        ),
        ContentEntry(
            role = Role.user,
            content = listOf(InputContent.Text(
                this.customizations.systemPrompt(SEG_RECOGNITION_USER, DEFAULT_RECOGNITION_USER)
                    .replace("{phone_language}", this.phoneLanguage)
            )),
        )
    )
)

internal fun ChatGPTResponse.toFoodRecognitionResponse(): FoodRecognitionResult {
    val gson = GsonBuilder().create()

    val resultJson: String? = this.output
        .lastOrNull {
            it.role == Role.assistant &&
                    it.content?.any { it is OutputContent.Text } == true
        }
        ?.content
        ?.filterIsInstance<OutputContent.Text>()
        ?.firstOrNull {
            it.text.isNotBlank()
        }
        ?.text

    val cachedTokens = this.usage.inputTokensDetails.cachedTokens

    class FoodDescription(
        @SerializedName("title") val title: String?,
        @SerializedName("error") val error: String?,
    )

    return resultJson
        ?.let {
            val foodDescription = gson.fromJson(resultJson, FoodDescription::class.java)
            if (foodDescription.error != null) {
                throw ChatGPTApiError.GenericApiError(foodDescription.error)
            }
            FoodRecognitionResult(
                title = foodDescription.title.takeIf { it.isNullOrBlank().not() },
                cachedTokens = cachedTokens,
            )
        }
        ?: FoodRecognitionResult(
            title = null,
            cachedTokens = cachedTokens,
        )
}

