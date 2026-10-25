package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.modal.model.RecordDetailsPristineSnapshot

/**
 * When edits to a logged meal send it back to the AI, and how an AI correction joins its description.
 */
internal object MealEditRules {

    /** A different set of photos needs a new analysis; the same photos in another order don't. */
    fun imagesRequireMacroReanalysis(pristine: List<String>, current: List<String>): Boolean =
        pristine.toSet() != current.toSet()

    /** Saving edits schedules a new nutrient analysis for new wording or a different set of photos. */
    fun editsRequireMacroAnalysis(
        pristine: RecordDetailsPristineSnapshot,
        title: String,
        description: String,
        imageFilenames: List<String>,
    ): Boolean =
        title.trim() != pristine.title.trim() ||
                description.trim() != pristine.description.trim() ||
                imagesRequireMacroReanalysis(pristine.imageFilenames, imageFilenames)

    /** The description the AI re-analyzes with: the existing one, followed by the user's correction on a new line. */
    fun appendCorrection(description: String, correction: String): String =
        listOf(description.trim(), correction.trim())
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n")

    /** How long a correction can be before the combined description exceeds [maxDescriptionLength]. */
    fun correctionCapacity(description: String, maxDescriptionLength: Int): Int {
        val existing = description.trim()
        val separatorLength = if (existing.isEmpty()) 0 else 1
        return (maxDescriptionLength - existing.length - separatorLength).coerceAtLeast(0)
    }
}
