package dev.gaborbiro.dailymacros.features.modal.model

import dev.gaborbiro.dailymacros.features.modal.ModalUiMapper
import dev.gaborbiro.dailymacros.features.shared.NutrientsUiMapper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutrientBreakdownUiModelTest {

    private val mapper = ModalUiMapper(NutrientsUiMapper())

    @Test
    fun `hasDisplayableContent is false when all fields are empty`() {
        val empty = NutrientBreakdownUiModel(
            calories = null,
            protein = null,
            fat = null,
            ofWhichSaturated = null,
            carbs = null,
            ofWhichSugar = null,
            ofWhichAddedSugar = null,
            salt = null,
            fibre = null,
            notes = null,
        )
        assertFalse(mapper.hasDisplayableContent(empty))
        assertFalse(mapper.hasDisplayableContent(empty.copy(notes = "")))
        assertFalse(mapper.hasDisplayableContent(empty.copy(calories = "   ")))
    }

    @Test
    fun `hasDisplayableContent is true when any macro or notes is present`() {
        val empty = NutrientBreakdownUiModel(
            calories = null,
            protein = null,
            fat = null,
            ofWhichSaturated = null,
            carbs = null,
            ofWhichSugar = null,
            ofWhichAddedSugar = null,
            salt = null,
            fibre = null,
            notes = null,
        )
        assertTrue(mapper.hasDisplayableContent(empty.copy(notes = "Leftovers")))
        assertTrue(mapper.hasDisplayableContent(empty.copy(calories = "Calories: 100 cal")))
    }
}
