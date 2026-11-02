package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.modal.model.RecordDetailsPristineSnapshot

/**
 * When edits to a logged meal send it back to the AI, and how the description shortcut opens.
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

    /** Put the cursor on a new line without changing an empty or full description. */
    fun descriptionForCorrection(description: String, maxDescriptionLength: Int): String = when {
        description.isBlank() || description.endsWith("\n") || description.length >= maxDescriptionLength -> description
        else -> "$description\n"
    }
}
