package dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model

data class WeeklyInsightsRequest(
    val diary: String,
    val customizations: Map<String, String> = emptyMap(),
)
