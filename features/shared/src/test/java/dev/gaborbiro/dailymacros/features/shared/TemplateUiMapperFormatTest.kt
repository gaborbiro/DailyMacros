package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.features.common.views.NutrientDisplayLine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Characterization tests pinning the exact output of [TemplateUiMapper]'s format functions,
 * so refactors and the move to shared number formatting can't change it unnoticed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class TemplateUiMapperFormatTest {

    private val mapper = TemplateUiMapper()
    private lateinit var originalLocale: Locale

    @Before
    fun pinLocale() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `each nutrient formats with its own decimals and unit`() {
        assertEquals("1240kcal", mapper.formatCalories(1240))
        assertEquals("13g", mapper.formatProtein(12.6f))
        assertEquals("20g", mapper.formatFat(20.4f, saturated = null))
        assertEquals("8g", mapper.formatSaturatedFat(8f))
        assertEquals("58g", mapper.formatCarbs(57.5f, sugar = null, addedSugar = null))
        assertEquals("12g", mapper.formatSugar(12f))
        assertEquals("3g", mapper.formatAddedSugar(3.4f))
        assertEquals("0.2g", mapper.formatSalt(0.25f))
        assertEquals("4g", mapper.formatFibre(4f))
    }

    @Test
    fun `fat appends saturated fat in parentheses`() {
        assertEquals("20g (8g)", mapper.formatFat(20.4f, saturated = 8f))
    }

    @Test
    fun `carbs append sugar and added sugar in parentheses`() {
        assertEquals("58g (12g)", mapper.formatCarbs(57.5f, sugar = 12f, addedSugar = null))
        assertEquals("58g (12g/3g)", mapper.formatCarbs(57.5f, sugar = 12f, addedSugar = 3.4f))
    }

    @Test
    fun `carbs ignore added sugar when sugar is missing`() {
        assertEquals("58g", mapper.formatCarbs(57.5f, sugar = null, addedSugar = 3.4f))
    }

    @Test
    fun `whole numbers keep only the decimals the nutrient requires`() {
        assertEquals("57g", mapper.formatCarbs(57f, sugar = null, addedSugar = null))
        assertEquals("2g", mapper.formatSalt(2f))
    }

    @Test
    fun `amounts round half to even`() {
        assertEquals("2g", mapper.formatProtein(2.5f))
        assertEquals("4g", mapper.formatProtein(3.5f))
        assertEquals("0.2g", mapper.formatSalt(0.25f))
        assertEquals("0.8g", mapper.formatSalt(0.75f))
    }

    @Test
    fun `top contributor suffix is shown at or above the smallest displayed step`() {
        assertEquals(" (chicken)", mapper.formatTopContributorSuffix(1f, NutrientDisplayLine.Protein, "chicken"))
        assertEquals("", mapper.formatTopContributorSuffix(0.4f, NutrientDisplayLine.Protein, "chicken"))
        assertEquals(" (soy)", mapper.formatTopContributorSuffix(0.1f, NutrientDisplayLine.Salt, "soy"))
    }

    @Test
    fun `topContributors is the bare text under the same visibility rule`() {
        assertEquals("chicken", mapper.topContributors(1f, NutrientDisplayLine.Protein, "chicken"))
        assertNull(mapper.topContributors(0.4f, NutrientDisplayLine.Protein, "chicken"))
        assertNull(mapper.topContributors(5f, NutrientDisplayLine.Protein, "  "))
        assertNull(mapper.topContributors(null, NutrientDisplayLine.Protein, "chicken"))
    }
}
