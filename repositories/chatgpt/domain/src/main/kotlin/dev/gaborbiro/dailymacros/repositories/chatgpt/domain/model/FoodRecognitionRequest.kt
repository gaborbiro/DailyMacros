package dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model

data class FoodRecognitionRequest(
    val base64Images: List<String>,
    val customizations: Map<String, String> = emptyMap(),
    val phoneLanguage: String,
)
