package dev.gaborbiro.dailymacros.repositories.chatgpt.service.model

import com.google.gson.annotations.SerializedName

data class ChatGPTResponse(
    @SerializedName("output") val output: List<ContentEntry<OutputContent>>,
    @SerializedName("usage") val usage: Usage,
)

data class ContentEntry<T>(
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

data class Usage(
    @SerializedName("input_tokens")
    val inputTokens: Int,

    @SerializedName("input_tokens_details")
    val inputTokensDetails: InputTokensDetails,

    @SerializedName("output_tokens")
    val outputTokens: Int,

    @SerializedName("output_tokens_details")
    val outputTokensDetails: OutputTokensDetails,

    @SerializedName("total_tokens")
    val totalTokens: Int,
)

data class InputTokensDetails(
    @SerializedName("cached_tokens")
    val cachedTokens: Int,
)

data class OutputTokensDetails(
    @SerializedName("reasoning_tokens")
    val reasoningTokens: Int,
)
