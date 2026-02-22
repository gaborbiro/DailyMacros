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
        model = model,
        input = listOf(
            ContentEntry(
                role = Role.system,
                content = listOf(InputContent.Text(SHARED_SYSTEM_PROMPT)),
            ),
            ContentEntry(
                role = Role.user,
                content = base64Images.map {
                    InputContent.Image(it)
                },
            ),
            ContentEntry(
                role = Role.user,
                content = listOf(
                    InputContent.Text(
                        """
TASK: MACRO_ESTIMATION

Use both images and provided text.
If text contradicts image, prefer text.

Title:
$title

Description:
$description

Output format:
{
  "macros": {
    "calories": 0.0,
    "protein": 0.0,
    "fat": 0.0,
    "ofWhichSaturated": 0.0,
    "carbohydrate": 0.0,
    "ofWhichSugar": 0.0,
    "ofWhichAddedSugar": 0.0,
    "salt": 0.00,
    "fibre": 0.0
  },
  "title": "",
  "notes": ""
}

If estimation is not possible:
{
  "error": ""
}
"""
                    )
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
                    it.content?.any { it is OutputContent.Text } == true
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
