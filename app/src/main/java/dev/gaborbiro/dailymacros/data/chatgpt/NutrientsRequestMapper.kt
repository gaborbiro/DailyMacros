package dev.gaborbiro.dailymacros.data.chatgpt

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import dev.gaborbiro.dailymacros.data.chatgpt.model.NutrientApiModel
import dev.gaborbiro.dailymacros.data.chatgpt.model.NutrientsRequest
import dev.gaborbiro.dailymacros.data.chatgpt.model.NutrientsResponse
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.InputContent
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.Role


internal fun NutrientsRequest.toApiModel(): ChatGPTRequest {
    return ChatGPTRequest(
        input = listOf(
            ContentEntry(
                role = Role.system,
                content = listOf(
                    InputContent.Text(
                        """
                            You are an intelligent image and text analyser for a nutrient tracker app.  
                            The user provides:
                            1. A photo of a meal or drink, AND/OR  
                            2. A title and description (often produced by a previous step in this app).  

                            Your job: estimate the nutrient content for the item.

                            TASK:
                            - Estimate the following nutrients:
                              • calories (cal)  
                              • protein (g)  
                              • carbohydrate (g)  
                              • ofWhichSugars (g)  
                              • fats (g)  
                              • ofWhichSaturatedFats (g)  
                              • salt (g)  
                              • fibre (g)  

                            - Only include nutrients you can reasonably estimate.  
                            - Round all numbers to **1 decimal place**.  
                            - If a nutrient is completely unknown but the rest can be estimated, omit that nutrient field rather than guessing wildly.

                            CONFIDENCE RULES:
                            - Output `"nutrients"` only if:
                              1. You are highly confident the photo shows real food or drink.  
                              2. The title/description provide enough detail to make a reasonable nutrient estimate (e.g., type of food, preparation, ingredients).  
                            - If either condition is not met:
                              - Omit `"nutrients"`.
                              - Include `"issues"` with a short, clear sentence explaining what’s missing or unclear so the user can improve their input.

                            ACCURACY PRINCIPLE:
                            - Perfect accuracy is less important than **reliability and consistency** in estimates across different meals.  
                            - Use typical/average nutritional values for standard foods when exact packaging data is unavailable.

                            OUTPUT FORMAT:
                            Always return valid JSON with no trailing commas.

                            If nutrients can be estimated:
                            {
                              "nutrients": {
                                "calories": 350.0,
                                "protein": 5.6,
                                "carbohydrate": 54.2,
                                "ofWhichSugars": 20.7,
                                "fats": 12.8,
                                "ofWhichSaturatedFats": 12.8,
                                "salt": 5.4,
                                "fibre": 5.0
                              }
                            }

                            If nutrients cannot be estimated:
                            {
                              "issues": "No clear food item visible in the image. Provide a clearer photo or detailed description."
                            }

                            NOTES:
                            - Always use both the image and the text provided.
                            - If values are extremely uncertain (e.g., ambiguous dish), consider omitting `"nutrients"` entirely and use `"issues"` instead.
                        """.trimIndent()
                    )
                ),
            ),
            ContentEntry(
                role = Role.user,
                content = listOfNotNull(
                    base64Image?.let { InputContent.Image(base64Image) },
                    InputContent.Text(title),
                    InputContent.Text(description),
                )
            )
        )
    )
}

private val gson = GsonBuilder().create()

internal fun ChatGPTResponse.toNutrientsResponse(): NutrientsResponse {
    val resultJson: String? = this.output
        .lastOrNull {
            it.role == Role.assistant &&
                    it.content.any { it is OutputContent.Text }
        }
        ?.content
        ?.filterIsInstance<OutputContent.Text>()
        ?.firstOrNull {
            it.text.isNotBlank() && it.text != "null"
        }
        ?.text

    class Nutrients(
        @SerializedName("calories") val calories: Number?,
        @SerializedName("protein") val protein: Number?,
        @SerializedName("carbohydrate") val carbs: Number?,
        @SerializedName("ofWhichSugars") val ofWhichSugars: Number?,
        @SerializedName("fats") val fats: Number?,
        @SerializedName("ofWhichSaturatedFats") val ofWhichSaturated: Number?,
        @SerializedName("salt") val salt: Number?,
        @SerializedName("fibre") val fibre: Number?,
    )

    class Response(
        @SerializedName("nutrients") val nutrients: Nutrients?,
        @SerializedName("issues") val issues: String?,
    )

    val response = gson.fromJson(resultJson, Response::class.java)

    val nutrients = response.nutrients?.let {
        NutrientApiModel(
            calories = response.nutrients.calories?.toInt(),
            proteinGrams = response.nutrients.protein?.toFloat(),
            carbGrams = response.nutrients.carbs?.toFloat(),
            ofWhichSugarGrams = response.nutrients.ofWhichSugars?.toFloat(),
            fatGrams = response.nutrients.fats?.toFloat(),
            ofWhichSaturatedGrams = response.nutrients.ofWhichSaturated?.toFloat(),
            saltGrams = response.nutrients.salt?.toFloat(),
            fibreGrams = response.nutrients.fibre?.toFloat(),
        )
    }
    return NutrientsResponse(
        nutrients = nutrients,
        issues = response.issues
    )
}
