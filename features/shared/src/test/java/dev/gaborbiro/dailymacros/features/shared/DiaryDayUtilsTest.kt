package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.features.common.utils.diaryDayWindowStart
import dev.gaborbiro.dailymacros.features.common.utils.logicalDiaryDate
import dev.gaborbiro.dailymacros.features.common.utils.nextDiaryDayBoundary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DiaryDayUtilsTest {

    private val paris = ZoneId.of("Europe/Paris")

    @Test
    fun logicalDiaryDate_midnight_behaves_like_calendar_date() {
        val dayStart = LocalTime.MIDNIGHT
        val zdt = ZonedDateTime.of(2024, 6, 10, 1, 30, 0, 0, paris)
        assertEquals(LocalDate.of(2024, 6, 10), zdt.logicalDiaryDate(dayStart))
    }

    @Test
    fun logicalDiaryDate_before_cutoff_counts_as_previous_day() {
        val dayStart = LocalTime.of(2, 0)
        val zdt = ZonedDateTime.of(2024, 6, 10, 1, 30, 0, 0, paris)
        assertEquals(LocalDate.of(2024, 6, 9), zdt.logicalDiaryDate(dayStart))
    }

    @Test
    fun logicalDiaryDate_at_cutoff_counts_as_same_day() {
        val dayStart = LocalTime.of(2, 0)
        val zdt = ZonedDateTime.of(2024, 6, 10, 2, 0, 0, 0, paris)
        assertEquals(LocalDate.of(2024, 6, 10), zdt.logicalDiaryDate(dayStart))
    }

    @Test
    fun diaryDayWindowStart_midnight_uses_atStartOfDay() {
        val day = LocalDate.of(2024, 6, 10)
        val z = diaryDayWindowStart(day, LocalTime.MIDNIGHT, paris)
        assertEquals(day, z.toLocalDate())
        assertEquals(LocalTime.MIDNIGHT, z.toLocalTime())
    }

    @Test
    fun nextDiaryDayBoundary_midnight_is_tomorrow_midnight() {
        val dayStart = LocalTime.MIDNIGHT
        val now = ZonedDateTime.of(2024, 6, 10, 15, 0, 0, 0, paris)
        val boundary = nextDiaryDayBoundary(now, dayStart)
        assertEquals(ZonedDateTime.of(2024, 6, 11, 0, 0, 0, 0, paris), boundary)
    }

    @Test
    fun nextDiaryDayBoundary_before_cutoff_is_later_today() {
        val dayStart = LocalTime.of(2, 0)
        val now = ZonedDateTime.of(2024, 6, 10, 1, 30, 0, 0, paris)
        val boundary = nextDiaryDayBoundary(now, dayStart)
        assertEquals(ZonedDateTime.of(2024, 6, 10, 2, 0, 0, 0, paris), boundary)
    }

    @Test
    fun nextDiaryDayBoundary_after_cutoff_is_tomorrow() {
        val dayStart = LocalTime.of(2, 0)
        val now = ZonedDateTime.of(2024, 6, 10, 10, 0, 0, 0, paris)
        val boundary = nextDiaryDayBoundary(now, dayStart)
        assertEquals(ZonedDateTime.of(2024, 6, 11, 2, 0, 0, 0, paris), boundary)
    }

    @Test
    fun nextDiaryDayBoundary_is_always_in_the_future() {
        val dayStart = LocalTime.of(2, 0)
        val now = ZonedDateTime.of(2024, 6, 10, 2, 0, 0, 0, paris)
        val boundary = nextDiaryDayBoundary(now, dayStart)
        assertEquals(ZonedDateTime.of(2024, 6, 11, 2, 0, 0, 0, paris), boundary)
    }
}
