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

internal fun FoodRecognitionRequest.toApiModel() = ChatGPTRequest(
    model = foodPhotoRecognitionModel,
    reasoning = ReasoningLevel(foodPhotoRecognitionReasoningEffort),
    input = listOf(
        ContentEntry(
            role = Role.system,
            content = listOf(InputContent.Text(sharedSystemPrompt())),
        ),
        ContentEntry(
            role = Role.user,
            content = base64Images.map {
                InputContent.Image(it)
            }
        ),
        ContentEntry(
            role = Role.user,
            content = listOf(
                InputContent.Text(
                    """
TASK: RECOGNITION

Return structured food breakdown suitable for later nutrient estimation.

Output format:
{
  "components": [
    {
      "name": "",
      "estimatedAmount": "",
      "confidence": "high|medium|low"
    }
  ],
  "title": "",
  "description": ""
}
If food cannot be determined:
{
  "error": "<one short sentence explaining clearly why food cannot be determined>"
}
"""
                )
            )
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

    // temporary helper class

    class FoodDescription(
        @SerializedName("title") val title: String?,
        @SerializedName("description") val description: String?,
        @SerializedName("components") val components: List<Component>,
        @SerializedName("error") val error: String?,
    )

    return resultJson
        ?.let {
            val foodDescription = gson.fromJson(resultJson, FoodDescription::class.java)
            if (foodDescription.error != null) {
                throw ChatGPTApiError.GenericApiError(foodDescription.error)
            }
            val componentStr = foodDescription.components.joinToString("\n") { component ->
                val confidence = when (component.confidence) {
                    "medium" -> "?"
                    "low" -> "??"
                    else -> null
                }
                "${component.estimatedAmount} ${component.name} ${confidence?.let { "($it)" } ?: ""}"
            }
            val descriptionItems = listOfNotNull(
                foodDescription.description.takeIf { it.isNullOrBlank().not() },
                componentStr
            )

            FoodRecognitionResult(
                title = foodDescription.title.takeIf { it.isNullOrBlank().not() },
                description = descriptionItems.joinToString("\nComponents:\n").takeIf { it.isNotBlank() },
                cachedTokens = cachedTokens,
            )
        }
        ?: FoodRecognitionResult(
            title = null,
            description = null,
            cachedTokens = cachedTokens,
        )
}
