package dev.gaborbiro.dailymacros.data.chatgpt.service.model

import com.google.gson.annotations.SerializedName

internal data class ChatGPTRequest(
    @SerializedName("model") val model: String = "gpt-4.1",
    @SerializedName("input") val input: List<ContentEntry<InputContent>>,
)
