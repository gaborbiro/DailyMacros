package dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model

data class NutrientAnalysisRequest(
    val base64Images: List<String>,
    val title: String,
    val description: String,
)
