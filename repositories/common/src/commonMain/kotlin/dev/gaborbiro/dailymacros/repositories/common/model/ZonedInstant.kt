package dev.gaborbiro.dailymacros.repositories.common.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class ZonedInstant(
    val instant: Instant,
    val timeZone: TimeZone,
) {
    val localDateTime: LocalDateTime get() = instant.toLocalDateTime(timeZone)
}
