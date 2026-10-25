package dev.gaborbiro.dailymacros.features.modal

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Appends [spoken] to whatever is already typed (so you can dictate in several goes) and puts
 * the cursor at the end. The recogniser returns lower-case text, so the first letter is
 * capitalised when it starts the field.
 */
internal fun appendSpokenText(current: TextFieldValue, spoken: String): TextFieldValue {
    val trimmed = spoken.trim()
    val existing = current.text.trimEnd()
    val newText = if (existing.isEmpty()) {
        trimmed.replaceFirstChar { it.uppercaseChar() }
    } else {
        "$existing $trimmed"
    }
    return TextFieldValue(text = newText, selection = TextRange(newText.length))
}
