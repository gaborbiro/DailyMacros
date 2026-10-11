package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class AppendSpokenTextTest {

    @Test
    fun `empty field takes the spoken text capitalised`() {
        val result = appendSpokenText(TextFieldValue(""), " two eggs on toast ")

        assertEquals("Two eggs on toast", result.text)
        assertEquals(TextRange(result.text.length), result.selection)
    }

    @Test
    fun `existing text gets the spoken text appended with a single space`() {
        val result = appendSpokenText(TextFieldValue("Porridge "), "with blueberries")

        assertEquals("Porridge with blueberries", result.text)
        assertEquals(TextRange(result.text.length), result.selection)
    }
}
