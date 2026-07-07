package dev.gaborbiro.dailymacros.features.shared

import android.util.Range
import dev.gaborbiro.dailymacros.features.common.views.NutrientDisplayLine
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class TemplateUiMapperTest {

    private val mapper = TemplateUiMapper()

    @Test
    fun `targetProgress divides total by max when no min`() {
        val target = Target(enabled = true, min = null, max = 80)
        assertEquals(0.25f, mapper.targetProgress(target, 20f)!!, 0f)
    }

    @Test
    fun `targetProgress null when max missing`() {
        val target = Target(enabled = true, min = 1, max = null)
        assertNull(mapper.targetProgress(target, 20f))
    }

    @Test
    fun `targetProgress normalises below min over 0-75 segment`() {
        // total=50, min=100, max=200 → (50/100)*0.75 = 0.375
        val target = Target(enabled = true, min = 100, max = 200)
        assertEquals(0.375f, mapper.targetProgress(target, 50f)!!, 0.0001f)
    }

    @Test
    fun `targetProgress normalises above min over 75-100 segment`() {
        // total=150, min=100, max=200 → 0.75 + (50/100)*0.25 = 0.875
        val target = Target(enabled = true, min = 100, max = 200)
        assertEquals(0.875f, mapper.targetProgress(target, 150f)!!, 0.0001f)
    }

    @Test
    fun `targetProgress at min equals 0_75`() {
        val target = Target(enabled = true, min = 100, max = 200)
        assertEquals(0.75f, mapper.targetProgress(target, 100f)!!, 0.0001f)
    }

    @Test
    fun `targetProgress at max equals 1_0`() {
        val target = Target(enabled = true, min = 100, max = 200)
        assertEquals(1.0f, mapper.targetProgress(target, 200f)!!, 0.0001f)
    }

    @Test
    fun `targetRange lower is 0_75 when both min and max set and min positive`() {
        val target = Target(enabled = true, min = 25, max = 100)
        val range: Range<Float> = mapper.targetRange(target)
        assertEquals(0.75f, range.lower, 0f)
        assertEquals(1f, range.upper, 0f)
    }

    @Test
    fun `targetRange lower zero when min or max missing`() {
        val onlyMax = Target(enabled = true, min = null, max = 50)
        assertEquals(0f, mapper.targetRange(onlyMax).lower, 0f)
        val onlyMin = Target(enabled = true, min = 10, max = null)
        assertEquals(0f, mapper.targetRange(onlyMin).lower, 0f)
    }

    @Test
    fun `targetRange lower zero when min is zero`() {
        val target = Target(enabled = true, min = 0, max = 100)
        assertEquals(0f, mapper.targetRange(target).lower, 0f)
    }

    @Test
    fun `formatTopContributorText blank yields empty`() {
        assertEquals("", mapper.formatTopContributorText(null))
        assertEquals("", mapper.formatTopContributorText("   "))
    }

    @Test
    fun `formatTopContributorText non blank wraps with newline indent`() {
        assertTrue(mapper.formatTopContributorText("eggs").contains("eggs"))
    }

    @Test
    fun `formatTopContributorSuffix empty when amount null`() {
        assertEquals("", mapper.formatTopContributorSuffix(null, NutrientDisplayLine.Protein, "x"))
    }

    @Test
    fun `formatTopContributorSuffix empty when below visibility threshold`() {
        assertEquals("", mapper.formatTopContributorSuffix(0.5f, NutrientDisplayLine.Protein, "x"))
    }

    @Test
    fun `formatTopContributorSuffix includes text when amount at threshold`() {
        val s = mapper.formatTopContributorSuffix(5f, NutrientDisplayLine.Protein, "chicken")
        assertTrue(s.contains("chicken"))
    }

    @Test
    fun `mapRecordNutrients formats calories and protein`() {
        val n = Nutrients(
            calories = 250,
            protein = 12f,
            fat = null,
            ofWhichSaturated = null,
            carbs = null,
            ofWhichSugar = null,
            ofWhichAddedSugar = null,
            salt = null,
            fibre = null,
        )
        val ui = mapper.mapRecordNutrients(n)
        assertNotNull(ui.calories)
        assertTrue(ui.calories!!.contains("250"))
        assertNotNull(ui.protein)
        assertTrue(ui.protein!!.contains("12"))
    }
}
