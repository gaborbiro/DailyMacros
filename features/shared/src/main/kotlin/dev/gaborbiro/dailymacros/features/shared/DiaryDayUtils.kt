package dev.gaborbiro.dailymacros.features.shared

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Wall-clock time when a new food diary day begins (inclusive). Values before this on the local
 * timeline belong to the previous diary day. Interpreted in each record’s own zone (same as
 * [java.time.ZonedDateTime.toLocalDate] at midnight).
 */
fun diaryDayStartTime(hourOfDay: Int): LocalTime =
    LocalTime.of(hourOfDay.coerceIn(0, 23), 0)

fun ZonedDateTime.logicalDiaryDate(dayStart: LocalTime): LocalDate {
    val civilDate = toLocalDate()
    val localTime = toLocalDateTime().toLocalTime()
    return if (localTime < dayStart) civilDate.minusDays(1) else civilDate
}

fun logicalDiaryToday(zone: ZoneId, dayStart: LocalTime): LocalDate =
    ZonedDateTime.now(zone).logicalDiaryDate(dayStart)

/**
 * First instant of diary day [day] in [zone]. Uses [LocalDate.atStartOfDay] at midnight so DST
 * gaps at 00:00 match prior behaviour; non-midnight starts use [ZonedDateTime.of].
 */
fun diaryDayWindowStart(day: LocalDate, dayStart: LocalTime, zone: ZoneId): ZonedDateTime =
    if (dayStart == LocalTime.MIDNIGHT) day.atStartOfDay(zone)
    else ZonedDateTime.of(day, dayStart, zone)
