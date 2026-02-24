package dev.gaborbiro.dailymacros.repo.chatgpt.prompts.food

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientAnalysisResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientApiModel
import dev.gaborbiro.dailymacros.repo.chatgpt.domain.model.NutrientsApiModel
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.InputContent
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.Role


internal fun NutrientAnalysisRequest.toApiModel() = ChatGPTRequest(
    model = llmModel,
    input = listOf(
        ContentEntry(
            role = Role.system,
            content = listOf(InputContent.Text(sharedSystemPrompt())),
        ),
        ContentEntry(
            role = Role.user,
            content = this.base64Images.map {
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
${this.title}

Description:
${this.description}

Output format:
{
  "nutrients": {
    "calories": 0.0,
    "protein": {
        grams: 0.0,
        topContributorIngredients: "",
    },
    "fat": {
        grams: 0.0,
        topContributorIngredients: "",
    },
    "ofWhichSaturated": {
        grams: 0.0,
        topContributorIngredients: "",
    },
    "carbohydrate": {
        grams: 0.0,
        topContributorIngredients: "",
    },
    "ofWhichSugar": {
        grams: 0.0,
        topContributorIngredients: "",
    },
    "ofWhichAddedSugar": {
        grams: 0.0,
        topContributorIngredients: "",
    },
    "salt": {
        grams: 0.00,
        topContributorIngredients: "",
    },
    "fibre": {
        grams: 0.0,
        topContributorIngredients: "",
    },
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
  "error": "<one short, specific sentence explaining what is missing or unclear>"
}
"""
                )
            )
        )
    )
)

internal fun ChatGPTResponse.toNutrientAnalysisResponse(): NutrientAnalysisResponse {
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

    // temporary helper classes

    data class Nutrient(
        @SerializedName("grams") val grams: Number?,
        @SerializedName("topContributorIngredients") val topContributorIngredients: String?,
    )

    class Nutrients(
        @SerializedName("calories") val calories: Number?,
        @SerializedName("protein") val protein: Nutrient?,
        @SerializedName("fat") val fat: Nutrient?,
        @SerializedName("ofWhichSaturated") val ofWhichSaturated: Nutrient?,
        @SerializedName("carbohydrate") val carbs: Nutrient?,
        @SerializedName("ofWhichSugar") val ofWhichSugar: Nutrient?,
        @SerializedName("ofWhichAddedSugar") val ofWhichAddedSugar: Nutrient?,
        @SerializedName("salt") val salt: Nutrient?,
        @SerializedName("fibre") val fibre: Nutrient?,
    )

    class Response(
        @SerializedName("nutrients") val nutrients: Nutrients?,
        @SerializedName("description") val description: String?,
        @SerializedName("components") val components: List<Component>?,
        @SerializedName("error") val error: String?,
    )

    val response = gson.fromJson(resultJson, Response::class.java)

    fun map(nutrient: Nutrient?): NutrientApiModel {
        return NutrientApiModel(
            grams = nutrient?.grams?.toFloat(),
            topContributors = nutrient?.topContributorIngredients,
        )
    }

    val nutrients = response.nutrients?.let {
        // null value doesn't mean 0 for that macronutrient, the AI just has no information on it.
        NutrientsApiModel(
            calories = response.nutrients.calories?.toInt(),
            protein = map(response.nutrients.protein),
            fat = map(response.nutrients.fat),
            ofWhichSaturated = map(response.nutrients.ofWhichSaturated),
            carb = map(response.nutrients.carbs),
            ofWhichSugar = map(response.nutrients.ofWhichSugar),
            ofWhichAddedSugar = map(response.nutrients.ofWhichAddedSugar),
            salt = map(response.nutrients.salt),
            fibre = map(response.nutrients.fibre),
        )
    }

    val componentStr = response.components?.joinToString("\n") { component ->
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
        error = response.error,
        description = descriptionItems.joinToString("\nComponents:\n").takeIf { it.isNotBlank() },
    )
}
