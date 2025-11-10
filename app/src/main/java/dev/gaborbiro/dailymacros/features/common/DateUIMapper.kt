package dev.gaborbiro.dailymacros.features.common

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal class DateUIMapper {

    fun mapRecordTimestamp(timestamp: ZonedDateTime, forceDay: Boolean): String {
//        return when {
//            !timestamp.isBefore(ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)) -> {
//                if (forceDay) {
//                    "Today at ${timestamp.formatShortTimeOnly()}"
//                } else {
//                    timestamp.formatShortTimeOnly()
//                }
//            }
//
//            !timestamp.isBefore(ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(1)) -> {
//                if (forceDay) {
//                    "Yesterday at ${timestamp.formatShortTimeOnly()}"
//                } else {
//                    timestamp.formatShortTimeOnly()
//                }
//            }
//
//            else -> {
//                if (forceDay) {
//                    timestamp.formatShort()
//                } else {
//                    timestamp.formatShortTimeOnly()
//                }
//            }
//        }
        return if (forceDay) {
            timestamp.formatShort()
        } else {
            timestamp.formatShortTimeOnly()
        }
    }

    fun mapDayTitleTimestamp(localDate: LocalDate): String {
//        return when {
//            !localDate.isBefore(LocalDate.now()) -> {
//                "Today"
//            }
//
//            !localDate.isBefore(LocalDate.now().minusDays(1)) -> {
//                "Yesterday"
//            }
//
//            else -> localDate.formatShort()
//        }
        return localDate.formatShort()
    }

    //    fun ZonedDateTime.formatShort(): String = format(DateTimeFormatter.ofPattern("dd MMM, H:mm O"))
//    fun ZonedDateTime.formatShortTimeOnly(): String = format(DateTimeFormatter.ofPattern("H:mm O"))
    fun ZonedDateTime.formatShort(): String = format(DateTimeFormatter.ofPattern("dd MMM, H:mm"))
    fun ZonedDateTime.formatShortTimeOnly(): String = format(DateTimeFormatter.ofPattern("H:mm"))
    fun LocalDate.formatShort(): String = format(DateTimeFormatter.ofPattern("E, dd MMM"))
}
