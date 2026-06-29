package dev.gaborbiro.dailymacros.repositories.chatgpt.prompts

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.guardNotNull
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.InputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ReasoningLevel
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.Role


internal val DEFAULT_ANALYSIS_SYSTEM = """
You are a nutritional analyst for a macronutrient tracker app.
The user may provide:
• photos of a meal, possibly food label 
• title
• description

STRUCTURAL RULES:
- Round numbers to 1 decimal place, except salt (2 decimal places).
- If total fat is estimated, also estimate ofWhichSaturated.
- If carbohydrate is estimated, also estimate ofWhichSugar and ofWhichAddedSugar.
- ofWhichAddedSugar must never exceed ofWhichSugar.
- If vegetables, grains, legumes or seeds are present, estimate fibre.

ACCURACY RULES:
1. For any nutritional values that are clearly visible on packaging in the image, you MUST extract and use those exact values.
   - Do NOT estimate.
   - Do NOT adjust values.
   - Do NOT reinterpret.
   - Copy values exactly as shown (convert units if necessary). 
2. When ingredients are visible (meal photo or food label) or described but quantities are unclear, estimate typical portions.
3. When quantity is unclear, prefer to underestimate rather than overestimate.
4. If some required macronutrients are NOT shown on packaging:
   - You MUST estimate the missing values using typical nutritional knowledge
   - Missing values must NEVER default to 0 unless the packaging explicitly states 0
   - Clearly indicate in "notes" which values were taken from packaging and which were estimated.

OUTPUT RULES:
Use this JSON format:
{
  "nutrients": {
    "calories": 0.0,
    "protein": { 
      "grams": 0.0, 
      "topContributorIngredients": ""
    },
    "fat": { 
      "grams": 0.0, 
      "topContributorIngredients": ""
    },
    "ofWhichSaturated": {
      "grams": 0.0,
      "topContributorIngredients": ""
    },
    "carbohydrate": { 
      "grams": 0.0,
      "topContributorIngredients": ""
    },
    "ofWhichSugar": { 
      "grams": 0.0,
      "topContributorIngredients": ""
    },
    "ofWhichAddedSugar": {
      "grams": 0.0,
      "topContributorIngredients": ""
    },
    "salt": {
      "grams": 0.00,
      "topContributorIngredients": ""
    },
    "fibre": {
      "grams": 0.0,
      "topContributorIngredients": ""
      }
  },
  "components": [
      { 
        "name": "", 
        "estimatedAmount": "", 
        "confidence": "high|medium|low"
      }
  ],
  "title": "",
  "notes": "",
  "representative_of_meal": [ true, false ]
}

"representative_of_meal" RULES:
The "representative_of_meal" array MUST have the same length and order as the user-submitted meal photos (index 0 = first photo). Each entry is true if that photo clearly shows the prepared dish or at least some food that belongs to that dish; false for nutrition labels only, packaging-only shots, unrelated scenes, receipts, people, empty plates, or when unclear. If you omit "representative_of_meal", every image is treated as unknown for that classification downstream.

"topContributorIngredients" RULES:
List out those ingredients that meaningfully contributed to the estimation, in decreasing order of nutritional significance. Be brief, e.g. "bread" instead of "whole-grain sourdough bread".

"components" RULES:
List all (visible or assumed) components in
decreasing order of nutritional significance. This is useful for the user to double check that everything in the photos is accounted for. 

The output title will be used when no title is available yet. Leave it empty if input title is already specified. 

If analysis is not possible:
{
  "error": "<one short, specific sentence explaining what is missing or unclear>"
}

LANGUAGE RULES:
- All output (including title, description, notes and error message) MUST be in {phone_language}.
- Never switch output language based on packaging language.
- If packaging text is not in {phone_language}, translate into {phone_language} before returning output.
""".trimIndent()

internal val DEFAULT_ANALYSIS_USER = """
TASK: NUTRIENT ESTIMATION
Use both images and provided texts.
If texts contradicts images, prefer texts.

Title:
{title}

Description:
{description}
""".trimIndent()

internal fun NutrientAnalysisRequest.toApiModel() = ChatGPTRequest(
    model = customizations.systemPrompt(SEG_ANALYSIS_MODEL, nutrientAnalysisModel),
    reasoning = ReasoningLevel(customizations.systemPrompt(SEG_ANALYSIS_REASONING_EFFORT, nutrientAnalysisReasoningEffort)),
    input = listOf(
        ContentEntry(
            role = Role.system,
            content = listOf(
                InputContent.Text(
                    this.customizations.systemPrompt(SEG_ANALYSIS_SYSTEM, DEFAULT_ANALYSIS_SYSTEM)
                        .replace("{phone_language}", this.phoneLanguage)
                )
            ),
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
                        .replace("{title}", this.title)
                        .replace("{description}", this.description)
                )
            )
        )
    )
)

private val gson = GsonBuilder().create()

internal fun ChatGPTResponse.toNutrientAnalysisResponse(): NutrientAnalysisResponse {
    val resultJson = output
        .lastOrNull { it.role == Role.assistant && it.content?.any { it is OutputContent.Text } == true }.guardNotNull("Missing assistant content in ChatGPTResponse")
        .content.guardNotNull("Missing content in ChatGPTResponse")
        .filterIsInstance<OutputContent.Text>()
        .firstOrNull { it.text.isNotBlank() }.guardNotNull("Missing text entry in ChatGPTResponse")
        .text
    return gson.fromJson(resultJson, NutrientAnalysisResponse::class.java)
}

internal data class NutrientAnalysisResponse(
    @SerializedName("nutrients") val nutrients: NutrientsApiModel?,
    @SerializedName("title") val title: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("components") val components: List<ComponentApiModel>?,
    @SerializedName("representative_of_meal") val representativeOfMeal: List<Boolean>?,
    @SerializedName("error") val error: String?,
)

internal data class NutrientsApiModel(
    @SerializedName("calories") val calories: Number?,
    @SerializedName("protein") val protein: NutrientItemApiModel?,
    @SerializedName("fat") val fat: NutrientItemApiModel?,
    @SerializedName("ofWhichSaturated") val ofWhichSaturated: NutrientItemApiModel?,
    @SerializedName("carbohydrate") val carbs: NutrientItemApiModel?,
    @SerializedName("ofWhichSugar") val ofWhichSugar: NutrientItemApiModel?,
    @SerializedName("ofWhichAddedSugar") val ofWhichAddedSugar: NutrientItemApiModel?,
    @SerializedName("salt") val salt: NutrientItemApiModel?,
    @SerializedName("fibre") val fibre: NutrientItemApiModel?,
)

internal data class NutrientItemApiModel(
    @SerializedName("grams") val grams: Number?,
    @SerializedName("topContributorIngredients") val topContributorIngredients: String?,
)

internal data class ComponentApiModel(
    @SerializedName("name") val name: String?,
    @SerializedName("estimatedAmount") val estimatedAmount: String?,
    @SerializedName("confidence") val confidence: String?,
)
