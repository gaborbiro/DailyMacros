package dev.gaborbiro.dailymacros.repositories.chatgpt.service.model

import com.google.gson.annotations.SerializedName

data class ChatGPTRequest(
    @SerializedName("model") val model: String,
    @SerializedName("reasoning") val reasoning: ReasoningLevel,
    @SerializedName("text") val text: TextOptions = TextOptions(FormatType("json_object")),
    @SerializedName("input") val input: List<ContentEntry<InputContent>>,
)

data class ReasoningLevel(
    @SerializedName("effort") val effort: String, // none, minimal, low, medium, high, and xhigh
)

data class TextOptions(
    @SerializedName("format") val format: FormatType,
)

data class FormatType(
    @SerializedName("type") val type: String,
)

sealed class InputContent(
    @SerializedName("type") open val type: String,
) {
    data class Text(
        @SerializedName("text") val text: String,
    ) : InputContent("input_text")

    data class Image(
        @SerializedName("image_url") val base64Image: String,
    ) : InputContent("input_image")
}
