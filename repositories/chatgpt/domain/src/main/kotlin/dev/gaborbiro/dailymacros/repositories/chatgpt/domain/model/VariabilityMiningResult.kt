package dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model

/**
 * Raw JSON string from the assistant for meal variability mining (debug / preview; not persisted).
 */
data class VariabilityMiningResult(
    val profileJson: String,
)
