package dev.gaborbiro.dailymacros.repo.chatgpt.service.model

import com.google.gson.annotations.SerializedName

internal data class ContentEntry<T>(
    @SerializedName("role") val role: Role,
    @SerializedName("content") val content: List<T>,
)

enum class Role {
    system, user, assistant
}
