package dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model

data class FoodRecognitionResult(
    val title: String?,
    val description: String?,
    /** One flag per submitted photo index (0..n-1); from model `cover_photo` array. */
    val coverPhotoByImageIndex: List<Boolean>,
    val cachedTokens: Int,
)
