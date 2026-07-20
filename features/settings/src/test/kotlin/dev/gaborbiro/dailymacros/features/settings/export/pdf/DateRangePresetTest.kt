package dev.gaborbiro.dailymacros.features.settings.export.pdf

import dev.gaborbiro.dailymacros.repositories.settings.domain.model.DateRangePreset
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class DateRangePresetTest {

    // 2025-07-16 is a Wednesday.
    private val wednesday = LocalDate.of(2025, 7, 16)

    @Test
    fun `today is a single-day range`() {
        val range = computeRange(DateRangePreset.TODAY, wednesday, DayOfWeek.MONDAY)
        assertEquals(DiaryDateRange(wednesday, wednesday), range)
    }

    @Test
    fun `this week starts on the locale first day of week`() {
        val monday = computeRange(DateRangePreset.THIS_WEEK, wednesday, DayOfWeek.MONDAY)
        assertEquals(DiaryDateRange(LocalDate.of(2025, 7, 14), wednesday), monday)

        val sunday = computeRange(DateRangePreset.THIS_WEEK, wednesday, DayOfWeek.SUNDAY)
        assertEquals(DiaryDateRange(LocalDate.of(2025, 7, 13), wednesday), sunday)
    }

    @Test
    fun `last 7 days is inclusive of today`() {
        val range = computeRange(DateRangePreset.LAST_7_DAYS, wednesday, DayOfWeek.MONDAY)
        assertEquals(DiaryDateRange(LocalDate.of(2025, 7, 10), wednesday), range)
    }

    @Test
    fun `last week is the previous full week`() {
        val range = computeRange(DateRangePreset.LAST_WEEK, wednesday, DayOfWeek.MONDAY)
        assertEquals(DiaryDateRange(LocalDate.of(2025, 7, 7), LocalDate.of(2025, 7, 13)), range)
    }

    @Test
    fun `this month runs from the first to today`() {
        val range = computeRange(DateRangePreset.THIS_MONTH, wednesday, DayOfWeek.MONDAY)
        assertEquals(DiaryDateRange(LocalDate.of(2025, 7, 1), wednesday), range)
    }

    @Test
    fun `last 30 days is inclusive of today`() {
        val range = computeRange(DateRangePreset.LAST_30_DAYS, wednesday, DayOfWeek.MONDAY)
        assertEquals(DiaryDateRange(LocalDate.of(2025, 6, 17), wednesday), range)
    }

    @Test
    fun `last month is the previous calendar month`() {
        val range = computeRange(DateRangePreset.LAST_MONTH, wednesday, DayOfWeek.MONDAY)
        assertEquals(DiaryDateRange(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30)), range)
    }
}
