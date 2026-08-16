package dev.gaborbiro.dailymacros.repositories.chatgpt.service.model

import com.google.gson.annotations.SerializedName

/**
 * Body for [dev.gaborbiro.dailymacros.repositories.chatgpt.service.ChatGPTService.reportOutcome].
 * [feature] must be "recognition" or "analysis" (see PromptType).
 */
data class ReportOutcomeRequest(
    @SerializedName("feature") val feature: String,
)
