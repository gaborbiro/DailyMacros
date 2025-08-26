package dev.gaborbiro.dailymacros.repo.chatgpt.service.model

import com.google.gson.annotations.SerializedName

internal data class ChatGPTRequest(
    @SerializedName("model") val model: String,
    @SerializedName("text") val text: TextOptions = TextOptions(FormatType("json_object")),
    @SerializedName("input") val input: List<ContentEntry<InputContent>>,
)

// "gpt-4o-mini-2024-07-18" 5s, https://platform.openai.com/docs/models/gpt-4o-mini
// "gpt-4o-2024-08-06" 4s, guesses the low salt Kikkomani sauce, https://platform.openai.com/docs/models/gpt-4o
// gpt-4.1-nano-2025-04-14 3s, https://platform.openai.com/docs/models/gpt-4.1-nano


internal data class TextOptions(
    @SerializedName("format") val format: FormatType,
)

internal data class FormatType(
    @SerializedName("type") val type: String,
)
