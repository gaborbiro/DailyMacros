package dev.gaborbiro.dailymacros.repo.chatgpt

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosApiModel
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.MacrosResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.InputContent
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.Role


internal fun MacrosRequest.toApiModel(): ChatGPTRequest {
    return ChatGPTRequest(
        model = "gpt-5.1-2025-11-13",
        input = listOf(
            ContentEntry(
                role = Role.system,
                content = listOf(
                    InputContent.Text(
                        """
                            You are an intelligent image and text analyser for a macronutrient tracker app.  
                            The user provides:
                            1. Photos of a meal or drink, AND/OR  
                            2. A title and description (often produced by a previous step in this app).  

                            Your job: 
                            - Estimate the macronutrient content.
                            - Suggest one short title (around 3–4 words) that identify the food/drink, notwithstanding any title provided by the submitted request.

                            TASK:
                            - Estimate the following macronutrients:
                              • calories (cal)  
                              • protein (g)  
                              • carbohydrate (g)  
                              • ofWhichSugar (g)  
                              • ofWhichAddedSugar (g)
                              • fat (g)  
                              • ofWhichSaturated (g)  
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
                                "fat": 12.8,
                                "ofWhichSaturated": 12.8,
                                "carbohydrate": 54.2,
                                "ofWhichSugar": 20.7,
                                "ofWhichAddedSugar": 4.0,
                                "salt": 5.4,
                                "fibre": 5.0
                              },
                              "title": "",
                              "notes": "",
                            }

                            If macronutrients cannot be estimated:
                            {
                              "error": "<describe what the issue is with the photo>"
                            }
                            
                            ACCURACY PRINCIPLE:
                            - Perfect accuracy is less important than **reliability and consistency** in estimates across different meals.  
                            - Use typical/average nutritional values for standard foods when exact packaging data is unavailable.
                            - When packaging data is available but one or more macronutrients are missing from it, estimate them.
                            - When ingredients are listed but quantities are unknown, estimate ofWhichAddedSugars using typical proportions for that dish (e.g. teriyaki, BBQ, sweet chili, curry, tomato sauce).

                            CONFIDENCE RULES:
                            - Output "macros" only if:
                              1. You are highly confident the photos show real food or drink.  
                              2. The title/description and photos provide enough detail to make a reasonable macronutrient estimate (e.g., type of food, preparation, ingredients).  
                              3. If the photo is not primarily of food/drink but indirectly implies/references something edible (for ex a photo of an ice-cream van) then focus on the implied/referenced food/drink and one usual portion of it.
                            - If any of the above condition are not met:
                              - Omit "macros".
                              - Include "error" with a short, clear sentence explaining what’s missing or unclear so the user can improve their input.
                            - If total fat or total carbohydrates are estimated, you must also estimate ofWhichSaturated and ofWhichSugars using typical ratios, even if approximate.
                            - If vegetables, grains, legumes or seeds are visible or listed, always estimate fibre.
                            - If ofWhichSugars is estimated, also estimate ofWhichAddedSugars. Treat sugar from sauces, marinades, syrups, honey, sugar, sweeteners and processed condiments as added sugar. Treat sugar from fruit, vegetables, grains and milk as natural sugar. ofWhichAddedSugars must never exceed ofWhichSugars.

                            NOTES:
                            - Always use both the photos and the texts provided.
                            - If values are extremely uncertain (e.g., ambiguous dish), consider omitting `macros` entirely and use `error` instead.
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
        @SerializedName("fat") val fat: Number?,
        @SerializedName("ofWhichSaturated") val ofWhichSaturated: Number?,
        @SerializedName("carbohydrate") val carbs: Number?,
        @SerializedName("ofWhichSugar") val ofWhichSugar: Number?,
        @SerializedName("ofWhichAddedSugar") val ofWhichAddedSugar: Number?,
        @SerializedName("salt") val salt: Number?,
        @SerializedName("fibre") val fibre: Number?,
    )

    class Response(
        @SerializedName("macros") val macros: Macros?,
        @SerializedName("notes") val notes: String?,
        @SerializedName("title") val title: String?,
        @SerializedName("error") val error: String?,
    )

    val response = gson.fromJson(resultJson, Response::class.java)

    val macros = response.macros?.let {
        // null value doesn't mean 0 for that macronutrient, the AI just has no information on it.
        MacrosApiModel(
            calories = response.macros.calories?.toInt(),
            proteinGrams = response.macros.protein?.toFloat(),
            fatGrams = response.macros.fat?.toFloat(),
            ofWhichSaturatedGrams = response.macros.ofWhichSaturated?.toFloat(),
            carbGrams = response.macros.carbs?.toFloat(),
            ofWhichSugarGrams = response.macros.ofWhichSugar?.toFloat(),
            ofWhichAddedSugarGrams = response.macros.ofWhichAddedSugar?.toFloat(),
            saltGrams = response.macros.salt?.toFloat(),
            fibreGrams = response.macros.fibre?.toFloat(),
        )
    }
    return MacrosResponse(
        macros = macros,
        issues = response.error,
        notes = response.notes,
        title = response.title,
    )
}
