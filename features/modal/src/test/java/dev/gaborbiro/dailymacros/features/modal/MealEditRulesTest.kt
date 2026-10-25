package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.modal.model.RecordDetailsPristineSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealEditRulesTest {

    @Test
    fun `appendCorrection puts the correction on its own line after the description`() {
        assertEquals("Leftover rice\nIt was brown rice", MealEditRules.appendCorrection("Leftover rice ", " It was brown rice"))
    }

    @Test
    fun `appendCorrection without a description is just the correction`() {
        assertEquals("About 200 g", MealEditRules.appendCorrection("  ", "About 200 g"))
    }

    @Test
    fun `correctionCapacity leaves room for the separator`() {
        assertEquals(256 - 5 - 1, MealEditRules.correctionCapacity("Curry", maxDescriptionLength = 256))
        assertEquals(256, MealEditRules.correctionCapacity("", maxDescriptionLength = 256))
        assertEquals(0, MealEditRules.correctionCapacity("x".repeat(300), maxDescriptionLength = 256))
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
