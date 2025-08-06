package dev.gaborbiro.nutri.data.chatgpt

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import dev.gaborbiro.nutri.data.chatgpt.model.DomainError
import dev.gaborbiro.nutri.data.chatgpt.model.FoodPicSummaryRequest
import dev.gaborbiro.nutri.data.chatgpt.model.FoodPicSummaryResponse
import dev.gaborbiro.nutri.data.chatgpt.model.NutrientApiModel
import dev.gaborbiro.nutri.data.chatgpt.model.NutrientsRequest
import dev.gaborbiro.nutri.data.chatgpt.model.NutrientsResponse
import dev.gaborbiro.nutri.data.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.nutri.data.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.nutri.data.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.nutri.data.chatgpt.service.model.ContentEntry
import dev.gaborbiro.nutri.data.chatgpt.service.model.InputContent
import dev.gaborbiro.nutri.data.chatgpt.service.model.OutputContent
import dev.gaborbiro.nutri.data.chatgpt.service.model.Role

internal fun FoodPicSummaryRequest.toApiModel(): ChatGPTRequest {
    return ChatGPTRequest(
        input = listOf(
            ContentEntry(
                role = Role.system,
                content = listOf(
                    InputContent.Text(
                        "Assistant is an intelligent image analyser designed to help users keep track of their meals. The user will upload a photo of what they ate and Assistant suggests one, concise summary. The summary will be used as the title of food diary list entries, but later on it will also be used by Assistant (in conjunction with the same image) to estimate calories and macro-nutrients. It is up to the user to either accept the summary or write their own. Assistant formats its answer like the following example:\n" +
                                "{ \"summary\": \"Kefir (Low Fat, 500ml)\" }" +
                                "\nAssistant should only give an answer if it has high confidence that the photo is actually of food and it is food the user might have eaten. Otherwise simply return the string null instead of the json."
                    )
                ),
            ),
            ContentEntry(
                role = Role.user,
                content = listOf(
                    InputContent.Image(base64Image)
                )
            )
        )
    )
}

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
                                "    \"carbs\": 54,\n" +
                                "    \"fats\": 12,\n" +
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

internal fun ChatGPTResponse.toFoodPicSummaryResponse(): FoodPicSummaryResponse {
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

    class Summary(
        @SerializedName("summary") val summary: String,
    )
    return resultJson
        ?.let {
            val titleAndKCal = gson.fromJson(resultJson, Summary::class.java)
            FoodPicSummaryResponse(
                title = titleAndKCal.summary,
            )
        }
        ?: FoodPicSummaryResponse(null)
}

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
        @SerializedName("carbs") val carbs: Number?,
        @SerializedName("fats") val fats: Number?,
    )

    class Response(
        @SerializedName("nutrients") val nutrients: Nutrients?,
        @SerializedName("comments") val comments: String,
    )

    val response = gson.fromJson(resultJson, Response::class.java)

    val breakdown = response.nutrients?.let {
        NutrientApiModel(
            kcal = response.nutrients.calories?.toInt(),
            proteinGrams = response.nutrients.protein?.toFloat(),
            carbGrams = response.nutrients.carbs?.toFloat(),
            fatGrams = response.nutrients.fats?.toFloat()
        )
    }
    return NutrientsResponse(
        nutrients = breakdown,
        comments = response.comments
    )
}

internal fun ChatGPTApiError.toDomainModel(): DomainError {
    return when (this) {
        is ChatGPTApiError.AuthApiError -> DomainError.GoToSignInScreen(message, this)
        is ChatGPTApiError.InternetApiError -> DomainError.DisplayMessageToUser.CheckInternetConnection(this)
        is ChatGPTApiError.MappingApiError, is ChatGPTApiError.ContentNotFoundError -> DomainError.DisplayMessageToUser.ContactSupport(this)
        is ChatGPTApiError.GenericApiError -> message
            ?.let { DomainError.DisplayMessageToUser.Message(it, this) }
            ?: DomainError.DisplayMessageToUser.TryAgain(this)
    }
}
