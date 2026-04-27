package dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model

data class FoodRecognitionResult(
    val title: String?,
    val description: String?,
    /** One entry per submitted photo index; null when model omitted `cover_photo` or that index. */
    val coverPhotoByImageIndex: List<Boolean?>,
    val cachedTokens: Int,
)
