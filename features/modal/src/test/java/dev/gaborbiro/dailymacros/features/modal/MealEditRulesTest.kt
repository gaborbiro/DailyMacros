package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.modal.model.RecordDetailsPristineSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealEditRulesTest {

    @Test
    fun `description shortcut puts the cursor on one new line without rewriting the description`() {
        assertEquals("Leftover rice \n", MealEditRules.descriptionForCorrection("Leftover rice ", 256))
        assertEquals("Leftover rice\n", MealEditRules.descriptionForCorrection("Leftover rice\n", 256))
        assertEquals("", MealEditRules.descriptionForCorrection("", 256))
    }

    @Test
    fun `description shortcut never truncates existing text to make room for a newline`() {
        val full = "x".repeat(256)
        assertEquals(full, MealEditRules.descriptionForCorrection(full, 256))
        val almostFull = "x".repeat(255)
        assertEquals(almostFull + "\n", MealEditRules.descriptionForCorrection(almostFull, 256))
    }

    @Test
    fun `imagesRequireMacroReanalysis only for a different set of photos`() {
        assertFalse(MealEditRules.imagesRequireMacroReanalysis(listOf("a", "b"), listOf("a", "b")))
        assertFalse(MealEditRules.imagesRequireMacroReanalysis(listOf("a", "b"), listOf("b", "a")))
        assertTrue(MealEditRules.imagesRequireMacroReanalysis(listOf("a", "b"), listOf("a")))
        assertTrue(MealEditRules.imagesRequireMacroReanalysis(listOf("a", "b"), listOf("a", "b", "c")))
    }

    @Test
    fun `editsRequireMacroAnalysis ignores reordered photos and whitespace but not new wording or photos`() {
        val pristine = RecordDetailsPristineSnapshot(
            templateDbId = 1L,
            title = "Soup",
            description = "Lentil",
            imageFilenames = listOf("a", "b"),
        )

        fun requiresAnalysis(
            title: String = "Soup",
            description: String = "Lentil",
            images: List<String> = listOf("a", "b"),
        ) = MealEditRules.editsRequireMacroAnalysis(pristine, title, description, images)

        assertFalse(requiresAnalysis())
        assertFalse(requiresAnalysis(images = listOf("b", "a")))
        assertFalse(requiresAnalysis(title = "Soup "))
        assertTrue(requiresAnalysis(title = "Stew"))
        assertTrue(requiresAnalysis(description = "Lentil, no cream"))
        assertTrue(requiresAnalysis(images = listOf("a", "b", "c")))
    }
}
