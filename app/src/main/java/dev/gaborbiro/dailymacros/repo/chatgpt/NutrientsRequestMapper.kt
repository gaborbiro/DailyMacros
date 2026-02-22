package dev.gaborbiro.dailymacros.repo.chatgpt

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import dev.gaborbiro.dailymacros.features.modal.sha256
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientsApiModel
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientAnalysisResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.InputContent
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.Role


internal fun NutrientAnalysisRequest.toApiModel(): ChatGPTRequest {
    val request = ChatGPTRequest(
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
TASK: NUTRIENT_ESTIMATION

Use both images and provided text.
If text contradicts image, prefer text.

Title:
$title

Description:
$description

Output format:
{
  "nutrients": {
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
  "components": [
    {
      "name": "",
      "estimatedAmount": "",
      "confidence": "high|medium|low"
    }
  ],
  "description": ""
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
    Log.i("Request SHA-256", request.input.take(2).joinToString().sha256())
    return request
}

private val gson = GsonBuilder().create()

internal fun ChatGPTResponse.toNutrientAnalysisResponse(): NutrientAnalysisResponse {
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

    class Nutrients(
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
        @SerializedName("nutrients") val nutrients: Nutrients?,
        @SerializedName("description") val description: String?,
        @SerializedName("components") val components: List<Component>,
        @SerializedName("error") val error: String?,
    )

    val response = gson.fromJson(resultJson, Response::class.java)

    val nutrients = response.nutrients?.let {
        // null value doesn't mean 0 for that macronutrient, the AI just has no information on it.
        NutrientsApiModel(
            calories = response.nutrients.calories?.toInt(),
            proteinGrams = response.nutrients.protein?.toFloat(),
            fatGrams = response.nutrients.fat?.toFloat(),
            ofWhichSaturatedGrams = response.nutrients.ofWhichSaturated?.toFloat(),
            carbGrams = response.nutrients.carbs?.toFloat(),
            ofWhichSugarGrams = response.nutrients.ofWhichSugar?.toFloat(),
            ofWhichAddedSugarGrams = response.nutrients.ofWhichAddedSugar?.toFloat(),
            saltGrams = response.nutrients.salt?.toFloat(),
            fibreGrams = response.nutrients.fibre?.toFloat(),
        )
    }

    val componentStr = response.components.joinToString("\n") { component ->
        val confidence = when (component.confidence) {
            "medium" -> "?"
            "low" -> "??"
            else -> null
        }
        "${component.estimatedAmount} ${component.name} ${confidence?.let { "($it)" } ?: ""}"
    }
    val descriptionItems = listOfNotNull(
        response.description.takeIf { it.isNullOrBlank().not() },
        componentStr
    )

    return NutrientAnalysisResponse(
        nutrients = nutrients,
        issues = response.error,
        description = descriptionItems.joinToString("\nComponents:\n").takeIf { it.isNotBlank() },
    )
}
