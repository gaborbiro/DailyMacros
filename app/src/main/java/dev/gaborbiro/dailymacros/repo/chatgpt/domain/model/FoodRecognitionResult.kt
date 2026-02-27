package dev.gaborbiro.dailymacros.repo.chatgpt.domain.model

data class FoodRecognitionResult(
    val title: String?,
    val description: String?,
    val cachedTokens: Int,
)
