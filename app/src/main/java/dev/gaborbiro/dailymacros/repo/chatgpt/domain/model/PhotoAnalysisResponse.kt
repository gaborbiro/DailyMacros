package dev.gaborbiro.dailymacros.repo.chatgpt.domain.model

import com.google.gson.annotations.SerializedName

data class PhotoAnalysisResponse(
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
)
