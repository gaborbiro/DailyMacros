package dev.gaborbiro.dailymacros.repo.chatgpt.domain.model

import com.google.gson.annotations.SerializedName

data class FoodRecognitionResponse(
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
)
