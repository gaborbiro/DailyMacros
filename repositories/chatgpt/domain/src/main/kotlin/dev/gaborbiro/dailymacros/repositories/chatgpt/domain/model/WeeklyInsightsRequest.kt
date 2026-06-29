package dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model

data class WeeklyInsightsRequest(
    val diary: String,
    val customisations: Map<String, String> = emptyMap(),
    val phoneLanguage: String,
)
