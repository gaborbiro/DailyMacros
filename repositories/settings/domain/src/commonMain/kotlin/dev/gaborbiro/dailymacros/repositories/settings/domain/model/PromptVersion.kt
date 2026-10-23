package dev.gaborbiro.dailymacros.repositories.settings.domain.model

data class PromptVersion(
    val version: Int,
    val createdAt: Long,
    val customisations: Map<String, String>,
    val type: String = "",
)
