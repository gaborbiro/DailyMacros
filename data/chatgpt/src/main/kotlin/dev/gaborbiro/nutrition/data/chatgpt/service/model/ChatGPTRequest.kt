package dev.gaborbiro.nutrition.data.chatgpt.service.model

import com.google.gson.annotations.SerializedName

internal data class ChatGPTRequest(
    @SerializedName("model") val model: String = "gpt-3.5-turbo", // or use another model
    @SerializedName("messages") val messages: List<Message>,
)
