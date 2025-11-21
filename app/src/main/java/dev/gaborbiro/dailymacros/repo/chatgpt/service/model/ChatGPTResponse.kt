package dev.gaborbiro.dailymacros.repo.chatgpt.service.model

import com.google.gson.annotations.SerializedName

internal data class ChatGPTResponse(
    @SerializedName("output") val output: List<ContentEntry<OutputContent>>,
)

sealed class OutputContent(
    @SerializedName("type") open val type: String,
) {
    data class Text(
        @SerializedName("text") val text: String,
    ) : OutputContent("output_text")
}