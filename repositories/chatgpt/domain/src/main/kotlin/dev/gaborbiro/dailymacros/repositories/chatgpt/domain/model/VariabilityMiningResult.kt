package dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model

/**
 * Raw JSON string from the assistant for meal variability mining.
 * The app persists this (normalized tables + snapshot row) after a successful run.
 */
data class VariabilityMiningResult(
    val profileJson: String,
)
