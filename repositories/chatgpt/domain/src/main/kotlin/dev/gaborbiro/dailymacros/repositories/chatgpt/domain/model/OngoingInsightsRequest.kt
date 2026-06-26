package dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model

data class OngoingInsightsRequest(
    val diary: String,
    val customizations: Map<String, String> = emptyMap(),
)
