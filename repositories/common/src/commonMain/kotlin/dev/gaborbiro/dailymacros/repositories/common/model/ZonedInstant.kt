package dev.gaborbiro.dailymacros.repositories.common.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * A moment in time together with the time zone it was observed in: the multiplatform stand-in for
 * java.time.ZonedDateTime, which kotlinx-datetime deliberately doesn't provide. It mirrors how a
 * record is persisted (epoch millis + zone id), so the wall-clock values are derived rather than
 * stored and can never disagree with the instant.
 *
 * Android code converts at the boundary with [toZonedDateTime] / [toZonedInstant] (jvmMain).
 */
data class ZonedInstant(
    val instant: Instant,
    val timeZone: TimeZone,
) {
    val localDateTime: LocalDateTime get() = instant.toLocalDateTime(timeZone)

    val offset: UtcOffset get() = timeZone.offsetAt(instant)
}
