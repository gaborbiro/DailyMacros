package dev.gaborbiro.dailymacros.repositories.settings.domain.model

data class PromptUsageStats(
    val count: Int,
    val totalTokens: Long,
) {
    val averageTokens: Long get() = if (count > 0) totalTokens / count else 0L
}
