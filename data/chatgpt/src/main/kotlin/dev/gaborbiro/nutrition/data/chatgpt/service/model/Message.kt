package dev.gaborbiro.nutrition.data.chatgpt.service.model

import com.google.gson.annotations.SerializedName

internal data class Message(
    @SerializedName("role") val role: Role,
    @SerializedName("content") val content: String,
)

enum class Role {
    system, user, assistant
}