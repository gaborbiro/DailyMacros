package dev.gaborbiro.dailymacros.repositories.chatgpt.prompts

// Segment IDs — must match keys stored in SettingsRepository and rendered in the prompt editor UI.
internal const val SEG_RECOGNITION_APPROACH = "recognition_approach"
internal const val SEG_RECOGNITION_CONTEXT = "recognition_context"
internal const val SEG_ANALYSIS_PRINCIPLES = "analysis_principles"
internal const val SEG_ANALYSIS_CONTEXT = "analysis_context"
internal const val SEG_ANALYSIS_CONFLICT_RESOLUTION = "analysis_conflict_resolution"
internal const val SEG_ANALYSIS_CONTRIBUTOR_HINT = "analysis_contributor_hint"

internal val DEFAULT_RECOGNITION_APPROACH =
    "Identify what is shown and return a concise English title, a brief description, and a list of visible components with confidence levels."

internal val DEFAULT_ANALYSIS_PRINCIPLES = """
- Perfect accuracy is less important than reliability and consistency.
- When ingredients are visible or described but quantities are unclear, estimate typical portions.
- When quantity is unclear, prefer to underestimate rather than overestimate.
""".trimIndent()

internal val DEFAULT_ANALYSIS_CONFLICT_RESOLUTION =
    "Use both images and provided text.\nIf text contradicts image, prefer text."

internal val DEFAULT_ANALYSIS_CONTRIBUTOR_HINT =
    "In topContributorIngredients list out those ingredients that meaningfully contributed to the estimation, in decreasing order of significance. Be brief, e.g. \"bread\" instead of \"whole-grain sourdough bread\"."

internal fun Map<String, String>.segment(id: String, default: String): String =
    this[id]?.takeIf { it.isNotBlank() } ?: default

internal fun recognitionSystemPrompt(customizations: Map<String, String>): String = buildString {
    appendLine("You are a food identifier for a macronutrient tracker app.")
    appendLine("The user provides one or more photos of a meal, drink, or food item.")
    appendLine()
    appendLine(customizations.segment(SEG_RECOGNITION_APPROACH, DEFAULT_RECOGNITION_APPROACH))
    val context = customizations[SEG_RECOGNITION_CONTEXT]?.trim().orEmpty()
    if (context.isNotBlank()) {
        appendLine()
        appendLine("Additional context about the user's diet:")
        appendLine(context)
    }
    appendLine()
    appendLine(SHARED_LANGUAGE_RULES)
}

internal fun analysisSystemPrompt(customizations: Map<String, String>): String = buildString {
    appendLine("You are a nutritional analyst for a macronutrient tracker app.")
    appendLine()
    appendLine("The user may provide:")
    appendLine("• Photos of a meal or drink.")
    appendLine("• A title and/or description.")
    appendLine()
    appendLine("General principles:")
    appendLine(customizations.segment(SEG_ANALYSIS_PRINCIPLES, DEFAULT_ANALYSIS_PRINCIPLES))
    appendLine()
    appendLine(ANALYSIS_STRUCTURAL_RULES)
    appendLine()
    appendLine(ANALYSIS_ACCURACY_RULES)
    val context = customizations[SEG_ANALYSIS_CONTEXT]?.trim().orEmpty()
    if (context.isNotBlank()) {
        appendLine()
        appendLine("Additional context about the user's diet:")
        appendLine(context)
    }
    appendLine()
    appendLine(SHARED_LANGUAGE_RULES)
}

private val SHARED_LANGUAGE_RULES = """
LANGUAGE RULES:
- All output (including titles, descriptions, notes and error messages) MUST be in English.
- Never switch output language based on packaging language.
- If packaging text is not in English, translate relevant information into English before returning output.
""".trimIndent()

private val ANALYSIS_STRUCTURAL_RULES = """
Structural rules:
- Round numbers to 1 decimal place, except salt (2 decimal places).
- If total fat is estimated, also estimate ofWhichSaturated.
- If carbohydrate is estimated, also estimate ofWhichSugar and ofWhichAddedSugar.
- ofWhichAddedSugar must never exceed ofWhichSugar.
- If vegetables, grains, legumes or seeds are present, estimate fibre.
- Only return valid JSON.
""".trimIndent()

private val ANALYSIS_ACCURACY_RULES = """
ACCURACY RULES:
1. For any nutritional values that are clearly visible on packaging in the image, you MUST extract and use those exact values.
   - Do NOT estimate.
   - Do NOT adjust values.
   - Do NOT reinterpret.
   - Copy values exactly as shown (convert units if necessary).

2. If some required macronutrients are NOT shown on packaging:
   - You MUST estimate the missing values using typical nutritional knowledge.
   - Missing values must NEVER default to 0 unless the packaging explicitly states 0.
   - Clearly indicate in "notes" which values were taken from packaging and which were estimated.
""".trimIndent()
