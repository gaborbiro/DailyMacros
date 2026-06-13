package dev.gaborbiro.dailymacros.repositories.chatgpt.prompts

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.MealComponent
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.Nutrient
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysis
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.InputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ReasoningLevel
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.Role


internal fun NutrientAnalysisRequest.toApiModel() = ChatGPTRequest(
    model = nutrientAnalysisModel,
    reasoning = ReasoningLevel(nutrientAnalysisReasoningEffort),
    input = listOf(
        ContentEntry(
            role = Role.system,
            content = listOf(InputContent.Text(
                this.customizations.systemPrompt(SEG_ANALYSIS_SYSTEM, DEFAULT_ANALYSIS_SYSTEM)
                    .replace("{phone_language}", this.phoneLanguage)
            )),
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
                    this.customizations.systemPrompt(SEG_ANALYSIS_USER, DEFAULT_ANALYSIS_USER)
                        .replace("{phone_language}", this.phoneLanguage)
                        .replace("{title}", this.title.orEmpty())
                        .replace("{description}", this.description.orEmpty())
                )
            )
        )
    )
)

internal fun ChatGPTResponse.toNutrientAnalysisResponse(imageCount: Int): NutrientAnalysis {
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

    // temporary helper classes

    val response = gson.fromJson(resultJson, NutrientAnalysisResponse::class.java)

    fun map(nutrient: NutrientApiModel?): Nutrient {
        return Nutrient(
            grams = nutrient?.grams?.toFloat(),
            topContributors = nutrient?.topContributorIngredients,
        )
    }

    val nutrients = response.nutrients?.let {
        // null value doesn't mean 0 for that macronutrient, the AI just has no information on it.
        Nutrients(
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

    val structuredComponents = response.components.orEmpty().mapNotNull { component ->
        val name = component.name?.trim().orEmpty()
        if (name.isEmpty()) return@mapNotNull null
        MealComponent(
            name = name,
            estimatedAmount = component.estimatedAmount?.trim().orEmpty(),
            confidence = component.confidence?.trim().orEmpty().ifEmpty { "high" },
        )
    }
    val componentStr = structuredComponents.joinToString("\n") { component ->
        val confidence = when (component.confidence) {
            "medium" -> "?"
            "low" -> "??"
            else -> null
        }
        "${component.estimatedAmount} ${component.name} ${confidence?.let { "($it)" } ?: ""}"
    }
    val notesItems = listOfNotNull(
        response.notes.takeIf { it.isNullOrBlank().not() },
        componentStr?.let { "Components:\n$it" }
    )

    val representativeFlags =
        normalizeRepresentativeOfMealFlags(imageCount, response.representativeOfMeal)

    return NutrientAnalysis(
        nutrients = nutrients,
        title = response.title,
        notes = notesItems.joinToString("\n").takeIf { it.isNotBlank() },
        components = structuredComponents,
        isRepresentativeOfMealByImageIndex = representativeFlags,
        cachedTokens = cachedTokens,
        error = response.error,
    )
}

private fun normalizeRepresentativeOfMealFlags(imageCount: Int, fromModel: List<Boolean>?): List<Boolean?> {
    if (imageCount <= 0) return emptyList()
    if (fromModel == null) return List(imageCount) { null }
    return List(imageCount) { index -> fromModel.getOrNull(index) }
}

private data class NutrientAnalysisResponse(
    @SerializedName("nutrients") val nutrients: NutrientsApiModel?,
    @SerializedName("title") val title: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("components") val components: List<Component>?,
    @SerializedName("representative_of_meal") val representativeOfMeal: List<Boolean>?,
    @SerializedName("error") val error: String?,
)

private data class NutrientsApiModel(
    @SerializedName("calories") val calories: Number?,
    @SerializedName("protein") val protein: NutrientApiModel?,
    @SerializedName("fat") val fat: NutrientApiModel?,
    @SerializedName("ofWhichSaturated") val ofWhichSaturated: NutrientApiModel?,
    @SerializedName("carbohydrate") val carbs: NutrientApiModel?,
    @SerializedName("ofWhichSugar") val ofWhichSugar: NutrientApiModel?,
    @SerializedName("ofWhichAddedSugar") val ofWhichAddedSugar: NutrientApiModel?,
    @SerializedName("salt") val salt: NutrientApiModel?,
    @SerializedName("fibre") val fibre: NutrientApiModel?,
)

private data class NutrientApiModel(
    @SerializedName("grams") val grams: Number?,
    @SerializedName("topContributorIngredients") val topContributorIngredients: String?,
)

