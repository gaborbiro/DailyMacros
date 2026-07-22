package dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model

data class WeeklyInsightsResult(
    val insights: Map<String, String>,
    val weekAssessment: String? = null,
)
