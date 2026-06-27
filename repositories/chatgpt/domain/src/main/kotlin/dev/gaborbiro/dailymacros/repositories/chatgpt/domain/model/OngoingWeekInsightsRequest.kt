package dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model

data class OngoingWeekInsightsRequest(
    val diary: String,
    val customizations: Map<String, String> = emptyMap(),
    val phoneLanguage: String,
)
