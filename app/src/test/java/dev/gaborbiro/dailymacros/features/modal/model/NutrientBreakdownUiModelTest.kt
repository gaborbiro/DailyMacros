package dev.gaborbiro.dailymacros.features.modal.model

import dev.gaborbiro.dailymacros.features.modal.ModalUiMapper
import dev.gaborbiro.dailymacros.features.shared.TemplateUiMapper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutrientBreakdownUiModelTest {

    private val mapper = ModalUiMapper(TemplateUiMapper())

    @Test
    fun `hasDisplayableContent is false when all fields are empty`() {
        val empty = NutrientBreakdownUiModel(
            nutrients = emptyList(),
            notes = null,
        )
        assertFalse(mapper.hasDisplayableContent(empty))
        assertFalse(mapper.hasDisplayableContent(empty.copy(notes = "")))
        assertFalse(
            mapper.hasDisplayableContent(
                empty.copy(nutrients = listOf(NutrientRowUiModel(NutrientKind.CALORIES, "Calories", "   "))),
            ),
        )
    }

    @Test
    fun `hasDisplayableContent is true when any macro or notes is present`() {
        val empty = NutrientBreakdownUiModel(
            nutrients = emptyList(),
            notes = null,
        )
        assertTrue(mapper.hasDisplayableContent(empty.copy(notes = "Leftovers")))
        assertTrue(
            mapper.hasDisplayableContent(
                empty.copy(nutrients = listOf(NutrientRowUiModel(NutrientKind.CALORIES, "Calories", "100kcal"))),
            ),
        )
    }
}
