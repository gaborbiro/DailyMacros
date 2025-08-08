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
                        "Assistant is an intelligent image analyser designed to help users keep track of their meals. The user will upload a photo of what they ate along with title/description and Assistant replies with the amount of calories in kilo calories, protein, carbs and fats in grams. This information will be used to sum up the user's daily calorie and protein intake and help them manage their weight-loss or muscle-building better. Assistant formats its answer like the following example:\n" +
                                "{\n" +
                                "  \"nutrients\": {\n" +
                                "    \"calories\": 350,\n" +
                                "    \"protein\": 5,\n" +
                                "    \"carbohydrate\": 54,\n" +
                                "    \"ofWhichSugars\": 40,\n" +
                                "    \"fats\": 12,\n" +
                                "    \"ofWhichSaturatedFats\": 12,\n" +
                                "    \"salt\": 5,\n" +
                                "  },\n" +
                                "  \"comments\": \"Assistant needs a photo or a detailed description of the meal in order to estimate nutrients.\"\n" +
                                "}" +
                                "\nIn the comments the Assistant summs up what main components the meal consists of, so that the User can double check things, potentially editing the photo/title/description and retrying the query." +
                                "\nAssistant should only specify the nutrients object if it has high confidence that the photo is actually of food, that it is food the user might have eaten and that the request overall provides sufficient information to estimate nutrients. Otherwise Assistant omits the nutrients object and uses the comment to explain why it cannot make provide the information. Reliability and consistency in answers is more important than perfect accuracy. What the user should ultimately be able to achieve is decreasing/increasing their nutrient intake in a consistent manner."
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
    )

    class Response(
        @SerializedName("nutrients") val nutrients: Nutrients?,
        @SerializedName("comments") val comments: String,
    )

    val response = gson.fromJson(resultJson, Response::class.java)

    val breakdown = response.nutrients?.let {
        NutrientApiModel(
            calories = response.nutrients.calories?.toInt(),
            proteinGrams = response.nutrients.protein?.toFloat(),
            carbGrams = response.nutrients.carbs?.toFloat(),
            ofWhichSugarGrams = response.nutrients.ofWhichSugars?.toFloat(),
            fatGrams = response.nutrients.fats?.toFloat(),
            ofWhichSaturatedGrams = response.nutrients.ofWhichSaturated?.toFloat(),
            saltGrams = response.nutrients.salt?.toFloat(),
        )
    }
    return NutrientsResponse(
        nutrients = breakdown,
        comments = response.comments
    )
}
