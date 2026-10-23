package dev.gaborbiro.dailymacros.repositories.settings.domain.model

/**
 * Keys identifying the AI prompt types. Used as [PromptVersion.type] and for usage stat tracking.
 */
object PromptType {
    const val RECOGNITION = "recognition"
    const val ANALYSIS = "analysis"
    const val WEEKLY_INSIGHTS = "insights"
    const val ONGOING_WEEK_INSIGHTS = "ongoing_insights"
}
