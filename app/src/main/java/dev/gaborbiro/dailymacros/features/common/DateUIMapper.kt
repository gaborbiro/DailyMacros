package dev.gaborbiro.dailymacros.features.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal class DateUIMapper {

    fun map(localDateTime: LocalDateTime, forceDay: Boolean): String {
        return when {
            !localDateTime.isBefore(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)) -> {
                if (forceDay) {
                    "Today at ${localDateTime.formatShortTimeOnly()}"
                } else {
                    localDateTime.formatShortTimeOnly()
                }
            }

            !localDateTime.isBefore(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(1)) -> {
                if (forceDay) {
                    "Yesterday at ${localDateTime.formatShortTimeOnly()}"
                } else {
                    localDateTime.formatShortTimeOnly()
                }
            }

            else -> {
                if (forceDay) {
                    localDateTime.formatShort()
                } else {
                    localDateTime.formatShortTimeOnly()
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

    fun LocalDateTime.formatShort() = format(DateTimeFormatter.ofPattern("E, dd MMM, H:mm"))
    fun LocalDateTime.formatShortTimeOnly() = format(DateTimeFormatter.ofPattern("H:mm"))
    fun LocalDate.formatShort() = format(DateTimeFormatter.ofPattern("E, dd MMM"))
}
