package dev.gaborbiro.dailymacros.repo.chatgpt.domain.model

data class NutrientAnalysisRequest(
    val base64Images: List<String>,
    val title: String,
    val description: String,
)
