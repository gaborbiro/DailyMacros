package dev.gaborbiro.dailymacros.repositories.records

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTermMatcherTest {

    @Test
    fun `matches when the full phrase appears contiguously`() {
        assertTrue(matchesAnySearchWord("chicken rice", "Chicken rice bowl", ""))
    }

    @Test
    fun `matches when words appear in reverse order`() {
        assertTrue(matchesAnySearchWord("rice chicken", "Chicken rice bowl", ""))
    }

    @Test
    fun `matches when only one of several words is present`() {
        assertTrue(matchesAnySearchWord("chicken pizza", "Chicken salad", ""))
    }

    @Test
    fun `matches against description as well as name`() {
        assertTrue(matchesAnySearchWord("leftovers", "Chicken salad", "Yesterday's leftovers"))
    }

    @Test
    fun `does not match when no word is present`() {
        assertFalse(matchesAnySearchWord("pizza burger", "Chicken salad", "with rice"))
    }

    @Test
    fun `blank search term matches nothing`() {
        assertFalse(matchesAnySearchWord("   ", "Chicken salad", ""))
    }

    @Test
    fun `is case insensitive`() {
        assertTrue(matchesAnySearchWord("CHICKEN", "chicken salad", ""))
    }
}
