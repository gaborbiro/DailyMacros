package dev.gaborbiro.dailymacros.features.modal.views.dictation

import androidx.compose.ui.text.TextRange
import org.junit.Assert.assertEquals
import org.junit.Test

class AppendSpokenTextTest {

    @Test
    fun `empty field takes the spoken text capitalised`() {
        val result = appendSpokenText("", listOf(" two eggs on toast "))

        assertEquals("Two eggs on toast", result.text)
        assertEquals(TextRange(result.text.length), result.selection)
    }

    @Test
    fun `existing text gets the spoken text appended with a single space`() {
        val result = appendSpokenText("Porridge ", listOf("with blueberries"))

        assertEquals("Porridge with blueberries", result.text)
        assertEquals(TextRange(result.text.length), result.selection)
    }

    @Test
    fun `finished segments and the in-progress partial are joined in order`() {
        val result = appendSpokenText("", listOf("two eggs", "and a latte", "with oat"))

        assertEquals("Two eggs and a latte with oat", result.text)
    }

    @Test
    fun `nothing spoken yet leaves the field as it was`() {
        val result = appendSpokenText("Porridge ", listOf("", "  "))

        assertEquals("Porridge ", result.text)
        assertEquals(TextRange(result.text.length), result.selection)
    }
}
