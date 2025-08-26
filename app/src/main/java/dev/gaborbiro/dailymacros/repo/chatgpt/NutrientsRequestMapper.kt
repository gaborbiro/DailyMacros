package dev.gaborbiro.dailymacros.repo.chatgpt

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import dev.gaborbiro.dailymacros.repo.chatgpt.model.MacrosApiModel
import dev.gaborbiro.dailymacros.repo.chatgpt.model.MacrosRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.model.MacrosResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.InputContent
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.Role


internal fun MacrosRequest.toApiModel(): ChatGPTRequest {
    return ChatGPTRequest(
        model = "gpt-4.1-2025-04-14",
        input = listOf(
            ContentEntry(
                role = Role.system,
                content = listOf(
                    InputContent.Text(
                        """
                            You are an intelligent image and text analyser for a macronutrient tracker app.  
                            The user provides:
                            1. Photos of a meal or drink, AND/OR  
                            2. A title and optional description (often produced by a previous step in this app).  

                            Your job: estimate the macronutrient content.

                            TASK:
                            - Estimate the following macronutrients:
                              • calories (cal)  
                              • protein (g)  
                              • carbohydrate (g)  
                              • ofWhichSugars (g)  
                              • fats (g)  
                              • ofWhichSaturatedFats (g)  
                              • salt (g)  
                              • fibre (g)  

                            - Only include macronutrients you can reasonably estimate.  
                            - Round all numbers to **1 decimal place**, except salt, which should have **2 decimal places**.

                            OUTPUT FORMAT:
                            Always return valid JSON with no trailing commas. Only return the following json formats. No breakdowns or calculations outside this format.

                            If macronutrients can be estimated:
                            {
                              "macros": {
                                "calories": 350.0,
                                "protein": 5.6,
                                "fats": 12.8,
                                "ofWhichSaturatedFats": 12.8,
                                "carbohydrate": 54.2,
                                "ofWhichSugars": 20.7,
                                "salt": 5.4,
                                "fibre": 5.0
                              },
                              "notes": "",
                            }

                            If macronutrients cannot be estimated:
                            {
                              "issues": "No clear food item visible in the image. Provide a clearer photo or detailed description."
                            }
                            
                            ACCURACY PRINCIPLE:
                            - Perfect accuracy is less important than **reliability and consistency** in estimates across different meals.  
                            - Use typical/average nutritional values for standard foods when exact packaging data is unavailable.
                            - When packaging data is available but one or more macronutrients are missing from it, estimate them.

                            CONFIDENCE RULES:
                            - Output "macros" only if:
                              1. You are highly confident the photos show real food or drink.  
                              2. The title/description and photos provide enough detail to make a reasonable macronutrient estimate (e.g., type of food, preparation, ingredients).  
                            - If either condition is not met:
                              - Omit "macros".
                              - Include "issues" with a short, clear sentence explaining what’s missing or unclear so the user can improve their input.

                            NOTES:
                            - Always use both the photos and the texts provided.
                            - If values are extremely uncertain (e.g., ambiguous dish), consider omitting `macros` entirely and use `issues` instead.
                            - Any breakdowns, calculations or observations should go into the "notes" field.
                            - Mention in the notes the top contributor ingredient for each significant macronutrient.
                            - Separate multiple notes by newline.
                        """.trimIndent()
                    )
                ),
            ),
            ContentEntry(
                role = Role.user,
                content = base64Images.map { InputContent.Image(it) } + listOf(
                    InputContent.Text(title),
                    InputContent.Text(description),
                )
            )
        )
    )
}

private val gson = GsonBuilder().create()

internal fun ChatGPTResponse.toMacrosResponse(): MacrosResponse {
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

    class Macros(
        @SerializedName("calories") val calories: Number?,
        @SerializedName("protein") val protein: Number?,
        @SerializedName("fats") val fats: Number?,
        @SerializedName("ofWhichSaturatedFats") val ofWhichSaturated: Number?,
        @SerializedName("carbohydrate") val carbs: Number?,
        @SerializedName("ofWhichSugars") val ofWhichSugars: Number?,
        @SerializedName("salt") val salt: Number?,
        @SerializedName("fibre") val fibre: Number?,
    )

    class Response(
        @SerializedName("macros") val macros: Macros?,
        @SerializedName("notes") val notes: String?,
        @SerializedName("issues") val issues: String?,
    )

    val response = gson.fromJson(resultJson, Response::class.java)

    val macros = response.macros?.let {
        // null value doesn't mean 0 for that macronutrient, the AI just has no information on it.
        MacrosApiModel(
            calories = response.macros.calories?.toInt(),
            proteinGrams = response.macros.protein?.toFloat(),
            fatGrams = response.macros.fats?.toFloat(),
            ofWhichSaturatedGrams = response.macros.ofWhichSaturated?.toFloat(),
            carbGrams = response.macros.carbs?.toFloat(),
            ofWhichSugarGrams = response.macros.ofWhichSugars?.toFloat(),
            saltGrams = response.macros.salt?.toFloat(),
            fibreGrams = response.macros.fibre?.toFloat(),
        )
    }
    return MacrosResponse(
        macros = macros,
        issues = response.issues,
        notes = response.notes,
    )
}
