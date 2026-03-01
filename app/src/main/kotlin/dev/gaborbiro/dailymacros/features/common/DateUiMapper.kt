package dev.gaborbiro.dailymacros.features.common

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal class DateUiMapper {

    fun mapRecordTimestamp(timestamp: ZonedDateTime, timeOnly: Boolean): String {
        return if (timeOnly) {
            timestamp.formatTimeOnly()
        } else {
            timestamp.format()
        }
    }

    fun mapDayTitleTimestamp(localDate: LocalDate): String {
        return localDate.format()
    }

    fun ZonedDateTime.format(): String = format(DateTimeFormatter.ofPattern("dd MMM, H:mm"))
    fun ZonedDateTime.formatTimeOnly(): String = format(DateTimeFormatter.ofPattern("H:mm"))
    fun LocalDate.format(): String = format(DateTimeFormatter.ofPattern("E, dd MMM"))
}
