package dev.gaborbiro.dailymacros.repo.chatgpt

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.FoodPicSummaryRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.FoodPicSummaryResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.InputContent
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.Role

internal fun FoodPicSummaryRequest.toApiModel(): ChatGPTRequest {
    return ChatGPTRequest(
        model = "gpt-4.1-nano-2025-04-14",
        input = listOf(
            ContentEntry(
                role = Role.system,
                content = listOf(
                    InputContent.Text(
                        """
                            You are an intelligent image and text analyser for a macronutrient tracker app.  
                            The user uploads photos of what they ate or drank.  
                            Your job: suggest concise, accurate summaries of the food/drink in the images or a funny error message.

                            TASKS:
                            1. Suggest one or more short titles (around 3–4 words each) that identify the food/drink.  
                               - Keep them brief but informative.  
                            2. Also provide one descriptive sentence that lists the major macronutrient-relevant components you see.  
                               - Focus on items that significantly affect the nutritional profile.  
                               - Do NOT try to name every small garnish or decorative element.  
                               - Include packaging size only if it is a standard, recognisable variant (e.g., “250g ready meal”, “500ml kefir bottle”).  
                               - Ignore whether the item is partially consumed.
                               - Describe only the food and drink, don't include any other context like "a hand holding…" or "…on a plate".

                            LANGUAGE RULES:
                            - Write in English by default.  
                            - Use correct spelling and capitalisation for product names and proper nouns.  

                            OUTPUT FORMAT:
                            Always return a valid JSON object in these structures.
                            Success format:
                            {
                                "titles": ["text", "text"],
                                "description": "<text>"
                            }
                            Error format:
                            {
                                "error": "<text>"
                            }

                            CONFIDENCE RULES:
                            - If the photos are not primarily of food/drink but indirectly implies/references something edible (for ex a photo of an ice-cream van) then instead of taking the photo at face value, just focus the a usual variant and portion of the implied/referenced food or drink (1 scoop of vanilla ice-cream in the previous example).
                            - Only return the success response if you can actually discern food/drink from the photo
                            - Only return the success response if the images refer to one meal only.
                            - Otherwise, tell the user what's wrong with the photos and why you cannot discern food or drink from it. Be funny/whimsical about it. Use the error format.
                        """.trimIndent()
                    )
                ),
            ),
            ContentEntry(
                role = Role.user,
                content = base64Images.map {
                    InputContent.Image(it)
                }
            )
        )
    )
}

private val gson = GsonBuilder().create()

internal fun ChatGPTResponse.toFoodPicSummaryResponse(): FoodPicSummaryResponse {
    val resultJson: String? = this.output
        .lastOrNull {
            it.role == Role.assistant &&
                    it.content.any { it is OutputContent.Text }
        }
        ?.content
        ?.filterIsInstance<OutputContent.Text>()
        ?.firstOrNull {
            it.text.isNotBlank()
        }
        ?.text

    class Summary(
        @SerializedName("titles") val titles: List<String>?,
        @SerializedName("description") val description: String?,
        @SerializedName("error") val error: String?,
    )
    return resultJson
        ?.let {
            val summary = gson.fromJson(resultJson, Summary::class.java)
            if (summary.error != null) {
                throw ChatGPTApiError.GenericApiError(summary.error)
            }
            FoodPicSummaryResponse(
                titles = summary.titles ?: emptyList(),
                description = summary.description,
            )
        }
        ?: FoodPicSummaryResponse(
            titles = emptyList(),
            description = null,
        )
}
