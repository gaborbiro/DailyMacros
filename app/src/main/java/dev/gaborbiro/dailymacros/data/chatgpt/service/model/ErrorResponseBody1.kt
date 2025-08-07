package dev.gaborbiro.dailymacros.data.chatgpt.service.model

import com.google.gson.annotations.SerializedName

internal data class ErrorResponseBody1(
    @SerializedName("error") val error: ChatGPTError?,
)

internal data class ChatGPTError(
    @SerializedName("message") val message: String,
    @SerializedName("type") val type: String,
    @SerializedName("code") val code: String,
)
