package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class TravelDay(
    val records: List<Record>,
    val day: LocalDate,
    val firstLog: ZonedDateTime,
    val lastLog: ZonedDateTime,
) {
    val duration: Duration
        get() = Duration.between(
            day.atStartOfDay(startZone).toInstant(),
            day.plusDays(1).atStartOfDay(endZone).toInstant()
        )

    val startZone: ZoneId get() = firstLog.zone
    val endZone: ZoneId get() = lastLog.zone
}
