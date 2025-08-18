package dev.gaborbiro.dailymacros.repo.chatgpt.model

data class MacrosRequest(
    val base64Image: String?,
    val title: String,
    val description: String,
)
