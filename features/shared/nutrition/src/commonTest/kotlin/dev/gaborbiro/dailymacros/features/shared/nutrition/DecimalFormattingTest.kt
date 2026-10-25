package dev.gaborbiro.dailymacros.features.shared.nutrition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DecimalFormattingTest {

    private fun format(value: Double, decimalCount: Int, dropTrailingZeroes: Boolean, localeTag: String = "en-US") =
        formatDecimal(value, decimalCount, dropTrailingZeroes, localeTag)

    @Test
    fun zeroDecimals_showsNoDecimalSeparator() {
        assertEquals("57", format(57.0, decimalCount = 0, dropTrailingZeroes = true))
    }

    @Test
    fun keepsTrailingZeroes_whenNotDropping() {
        assertEquals("2.0", format(2.0, decimalCount = 1, dropTrailingZeroes = false))
    }

    @Test
    fun dropsTrailingZeroes_whenDropping() {
        assertEquals("2", format(2.0, decimalCount = 1, dropTrailingZeroes = true))
        assertEquals("2.5", format(2.5, decimalCount = 2, dropTrailingZeroes = true))
    }

    @Test
    fun roundsHalfToEven() {
        assertEquals("2", format(2.5, decimalCount = 0, dropTrailingZeroes = true))
        assertEquals("4", format(3.5, decimalCount = 0, dropTrailingZeroes = true))
        assertEquals("0.2", format(0.25, decimalCount = 1, dropTrailingZeroes = false))
        assertEquals("0.8", format(0.75, decimalCount = 1, dropTrailingZeroes = false))
    }

    @Test
    fun neverGroupsThousands() {
        assertEquals("1240", format(1240.0, decimalCount = 0, dropTrailingZeroes = true))
        assertEquals("1240", format(1240.0, decimalCount = 0, dropTrailingZeroes = true, localeTag = "de-DE"))
    }

    @Test
    fun usesTheLocalesDecimalSeparator() {
        assertEquals("0.3", format(0.3, decimalCount = 1, dropTrailingZeroes = false, localeTag = "en-US"))
        assertEquals("0,3", format(0.3, decimalCount = 1, dropTrailingZeroes = false, localeTag = "de-DE"))
    }

    @Test
    fun negativeDecimalCount_isRejected() {
        assertFailsWith<IllegalArgumentException> {
            format(1.0, decimalCount = -1, dropTrailingZeroes = true)
        }
    }
}
