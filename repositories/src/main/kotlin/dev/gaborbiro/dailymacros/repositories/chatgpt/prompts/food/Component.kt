package dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.food

import com.google.gson.annotations.SerializedName

data class Component(
    @SerializedName("name") val name: String?,
    @SerializedName("estimatedAmount") val estimatedAmount: String?,
    @SerializedName("confidence") val confidence: String?,
)
