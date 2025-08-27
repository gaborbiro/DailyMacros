package dev.gaborbiro.dailymacros.repo.chatgpt.model

import com.google.gson.annotations.SerializedName

data class FoodPicSummaryResponse(
    @SerializedName("titles") val titles: List<String>,
    @SerializedName("description") val description: String?,
)
