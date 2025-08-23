package dev.gaborbiro.dailymacros.repo.chatgpt.model

data class MacrosRequest(
    val base64Images: List<String>,
    val title: String,
    val description: String,
)
