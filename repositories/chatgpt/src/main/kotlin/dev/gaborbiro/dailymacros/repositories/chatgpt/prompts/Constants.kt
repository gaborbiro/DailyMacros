package dev.gaborbiro.dailymacros.repositories.chatgpt.prompts

// none, minimal, low, medium, high, and xhigh

const val foodPhotoRecognitionModel = "gpt-5.4-nano"
const val foodPhotoRecognitionReasoningEffort = "low"

const val nutrientAnalysisModel = "gpt-5.4-mini"
const val nutrientAnalysisReasoningEffort = "high"




internal const val SEG_RECOGNITION_SYSTEM = "recognition_system"
internal const val SEG_RECOGNITION_USER = "recognition_user"
internal const val SEG_ANALYSIS_SYSTEM = "analysis_system"
internal const val SEG_ANALYSIS_USER = "analysis_user"

internal fun Map<String, String>.systemPrompt(id: String, default: String): String =
    this[id]?.takeIf { it.isNotBlank() } ?: default
