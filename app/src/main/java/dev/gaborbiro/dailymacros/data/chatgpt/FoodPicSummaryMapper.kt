package dev.gaborbiro.dailymacros.data.chatgpt

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import dev.gaborbiro.dailymacros.data.chatgpt.model.FoodPicSummaryRequest
import dev.gaborbiro.dailymacros.data.chatgpt.model.FoodPicSummaryResponse
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.ChatGPTRequest
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.ChatGPTResponse
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.InputContent
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.Role

internal fun FoodPicSummaryRequest.toApiModel(): ChatGPTRequest {
    return ChatGPTRequest(
        input = listOf(
            ContentEntry(
                role = Role.system,
                content = listOf(
                    InputContent.Text(
                        "Assistant is an intelligent image analyser designed to help users keep track of their meals. " +
                                "The user will upload a photo of what they ate and Assistant suggests one, concise summary" +
                                "of the overall meal. Don't mention if the meal/food is partially consumed on the photo." +
                                "The summary will be used as the title of food diary list entries, but later on it will also " +
                                "be used by Assistant (in conjunction with the same image) to estimate calories and macro-nutrients. " +
                                "It is up to the user to either accept the summary or write their own. " +
                                "Assistant speaks in English, but if the photo contains " +
                                "a good label that describes well what the food/meal is in a non English language, that label should also " +
                                "be returned so that the user can choose." +
                                "\nAssistant formats its answer like the following example: { \"summary\": [\"Kefir (Low Fat, 500ml)\", \"Kefír (csökkentett zsírtartalmú, 500ml)\"] }\n" +
                                "Assistant should only give an answer if it has high confidence that the photo is actually of " +
                                "food and it is food the user might have eaten. Otherwise return { \"summary\": [] }."
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
            it.text.isNotBlank()
        }
        ?.text

    class Summary(
        @SerializedName("summary") val summary: List<String>,
    )
    return resultJson
        ?.let {
            val titleAndKCal = gson.fromJson(resultJson, Summary::class.java)
            FoodPicSummaryResponse(
                summary = titleAndKCal.summary,
            )
        }
        ?: FoodPicSummaryResponse(emptyList())
}
