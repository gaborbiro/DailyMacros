package dev.gaborbiro.dailymacros.features.shared.model

import dev.gaborbiro.dailymacros.features.common.utils.diaryDayWindowStart
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class TravelDay(
    val records: List<Record>,
    val day: LocalDate,
    val firstLog: ZonedDateTime,
    val lastLog: ZonedDateTime,
    /** Local wall-clock time when this diary day begins in [startZone] / [endZone]. */
    val diaryDayStart: LocalTime = LocalTime.MIDNIGHT,
) {
    val duration: Duration
        get() = durationWithEndZone(endZone)

    fun durationWithEndZone(zone: ZoneId): Duration = Duration.between(
        diaryDayWindowStart(day, diaryDayStart, startZone).toInstant(),
        diaryDayWindowStart(day.plusDays(1), diaryDayStart, zone).toInstant(),
    )

    val startZone: ZoneId get() = firstLog.zone
    val endZone: ZoneId get() = lastLog.zone
}
