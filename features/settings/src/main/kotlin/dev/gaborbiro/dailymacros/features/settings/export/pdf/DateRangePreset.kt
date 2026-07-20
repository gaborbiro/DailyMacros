package dev.gaborbiro.dailymacros.features.settings.export.pdf

import dev.gaborbiro.dailymacros.repositories.settings.domain.model.DateRangePreset
import java.time.DayOfWeek
import java.time.LocalDate

/** Inclusive logical-diary-date range [from]..[to]. */
data class DiaryDateRange(val from: LocalDate, val to: LocalDate)

/**
 * Resolves a [DateRangePreset] to a concrete range in logical diary dates (the caller passes [today]
 * already resolved to a logical diary date, so all ranges honour the diary-day cutoff). "Week"
 * boundaries follow [firstDayOfWeek] (device locale's first day of week).
 */
fun computeRange(
    preset: DateRangePreset,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
): DiaryDateRange {
    fun startOfWeek(date: LocalDate): LocalDate {
        val diff = (date.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        return date.minusDays(diff.toLong())
    }

    return when (preset) {
        DateRangePreset.TODAY ->
            DiaryDateRange(today, today)

        DateRangePreset.THIS_WEEK ->
            DiaryDateRange(startOfWeek(today), today)

        DateRangePreset.LAST_7_DAYS ->
            DiaryDateRange(today.minusDays(6), today)

        DateRangePreset.LAST_WEEK -> {
            val thisWeekStart = startOfWeek(today)
            DiaryDateRange(thisWeekStart.minusDays(7), thisWeekStart.minusDays(1))
        }

        DateRangePreset.THIS_MONTH ->
            DiaryDateRange(today.withDayOfMonth(1), today)

        DateRangePreset.LAST_30_DAYS ->
            DiaryDateRange(today.minusDays(29), today)

        DateRangePreset.LAST_MONTH -> {
            val firstOfThisMonth = today.withDayOfMonth(1)
            val firstOfLastMonth = firstOfThisMonth.minusMonths(1)
            DiaryDateRange(firstOfLastMonth, firstOfThisMonth.minusDays(1))
        }
    }
}
