package dev.gaborbiro.dailymacros.features.modal.views.dictation

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Builds a field's value while dictating: [base] is what was in the field when dictation started,
 * [spoken] the recognised pieces since (finished segments, then the in-progress partial). Pieces are
 * joined with single spaces after [base], so you can dictate in several goes. The recogniser returns
 * lower-case text, so the first letter is capitalised when it starts the field. Cursor goes to the end.
 */
internal fun appendSpokenText(base: String, spoken: List<String>): TextFieldValue {
    val pieces = spoken.map { it.trim() }.filter { it.isNotEmpty() }
    if (pieces.isEmpty()) return TextFieldValue(text = base, selection = TextRange(base.length))
    val joined = pieces.joinToString(" ")
    val existing = base.trimEnd()
    val newText = if (existing.isEmpty()) {
        joined.replaceFirstChar { it.uppercaseChar() }
    } else {
        "$existing $joined"
    }
    return TextFieldValue(text = newText, selection = TextRange(newText.length))
}
