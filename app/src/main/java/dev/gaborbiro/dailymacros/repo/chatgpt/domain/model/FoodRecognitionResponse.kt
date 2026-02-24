package dev.gaborbiro.dailymacros.repo.chatgpt.domain.model

data class FoodRecognitionResponse(
    val title: String?,
    val description: String?,
    val cachedTokens: Int,
)
