package dev.gaborbiro.dailymacros.repositories.chatgpt.prompts

// none, minimal, low, medium, high, and xhigh

const val foodPhotoRecognitionModel = "gpt-5.4-nano"
const val foodPhotoRecognitionReasoningEffort = "low"

const val nutrientAnalysisModel = "gpt-5.4-mini"
const val nutrientAnalysisReasoningEffort = "high"

const val weeklyInsightsModel = "gpt-5.4-mini"
const val weeklyInsightsReasoningEffort = "medium"




internal const val SEG_RECOGNITION_MODEL = "recognition_model"
internal const val SEG_RECOGNITION_REASONING_EFFORT = "recognition_reasoning_effort"
internal const val SEG_RECOGNITION_SYSTEM = "recognition_system"
internal const val SEG_RECOGNITION_USER = "recognition_user"
internal const val SEG_ANALYSIS_MODEL = "analysis_model"
internal const val SEG_ANALYSIS_REASONING_EFFORT = "analysis_reasoning_effort"
internal const val SEG_ANALYSIS_SYSTEM = "analysis_system"
internal const val SEG_ANALYSIS_USER = "analysis_user"
internal const val SEG_INSIGHTS_MODEL = "insights_model"
internal const val SEG_INSIGHTS_REASONING_EFFORT = "insights_reasoning_effort"
internal const val SEG_INSIGHTS_SYSTEM = "insights_system"
internal const val SEG_INSIGHTS_USER = "insights_user"

internal fun Map<String, String>.systemPrompt(id: String, default: String): String =
    this[id]?.takeIf { it.isNotBlank() } ?: default
