package dev.gaborbiro.dailymacros.features.common

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal class DateUIMapper {

    fun map(timestamp: ZonedDateTime, forceDay: Boolean): String {
        return when {
            !timestamp.isBefore(ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)) -> {
                if (forceDay) {
                    "Today at ${timestamp.formatShortTimeOnly()}"
                } else {
                    timestamp.formatShortTimeOnly()
                }
            }

            !timestamp.isBefore(ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(1)) -> {
                if (forceDay) {
                    "Yesterday at ${timestamp.formatShortTimeOnly()}"
                } else {
                    timestamp.formatShortTimeOnly()
                }
            }

            else -> {
                if (forceDay) {
                    timestamp.formatShort()
                } else {
                    timestamp.formatShortTimeOnly()
                }
            }
        }
    }

    fun map(localDate: LocalDate): String {
        return when {
            !localDate.isBefore(LocalDate.now()) -> {
                "Today"
            }

            !localDate.isBefore(LocalDate.now().minusDays(1)) -> {
                "Yesterday"
            }

            else -> localDate.formatShort()
        }
    }

    fun ZonedDateTime.formatShort() = format(DateTimeFormatter.ofPattern("E, dd MMM, H:mm"))
    fun ZonedDateTime.formatShortTimeOnly() = format(DateTimeFormatter.ofPattern("H:mm"))
    fun LocalDate.formatShort() = format(DateTimeFormatter.ofPattern("E, dd MMM"))
}
