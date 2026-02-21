package dev.gaborbiro.dailymacros.repo.chatgpt.service.model

import com.google.gson.annotations.SerializedName

internal data class ChatGPTResponse(
    @SerializedName("output") val output: List<ContentEntry<OutputContent>>,
)

internal data class ContentEntry<T>(
    @SerializedName("role") val role: Role? = null,
    @SerializedName("content") val content: List<T>? = null,
)

enum class Role {
    system, user, assistant
}

sealed class OutputContent(
    @SerializedName("type") open val type: String,
) {
    data class Text(
        @SerializedName("text") val text: String,
    ) : OutputContent(type = "output_text")
}