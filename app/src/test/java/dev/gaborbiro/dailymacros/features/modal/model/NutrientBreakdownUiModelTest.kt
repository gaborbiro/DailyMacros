package dev.gaborbiro.dailymacros.features.modal.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutrientBreakdownUiModelTest {

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
        assertFalse(empty.hasDisplayableContent())
        assertFalse(empty.copy(notes = "").hasDisplayableContent())
        assertFalse(empty.copy(calories = "   ").hasDisplayableContent())
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
        assertTrue(empty.copy(notes = "Leftovers").hasDisplayableContent())
        assertTrue(empty.copy(calories = "Calories: 100 cal").hasDisplayableContent())
    }
}
