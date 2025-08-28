package dev.gaborbiro.dailymacros.features.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal class DateUIMapper {

    fun map(localDateTime: LocalDateTime): String {
        return when {
            !localDateTime.isBefore(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)) -> {
                "Today at ${localDateTime.formatShortTime()}"
            }

            !localDateTime.isBefore(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(1)) -> {
                "Yesterday at ${localDateTime.formatShortTime()}"
            }

            else -> localDateTime.formatShort()
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

    fun LocalDateTime.formatShort() = format(DateTimeFormatter.ofPattern("E dd MMM, H:mm"))
    fun LocalDateTime.formatShortTime() = format(DateTimeFormatter.ofPattern("H:mm"))
    fun LocalDate.formatShort() = format(DateTimeFormatter.ofPattern("E dd MMM"))
}
