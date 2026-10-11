package dev.gaborbiro.dailymacros.repositories.common.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ZonedInstantTest {

    private val instant = Instant.parse("2024-07-01T12:00:00Z")

    @Test
    fun wallClockFollowsTheZone() {
        assertEquals(
            LocalDateTime(2024, 7, 1, 13, 0),
            ZonedInstant(instant, TimeZone.of("Europe/London")).localDateTime,
        )
        assertEquals(
            LocalDateTime(2024, 7, 1, 17, 30),
            ZonedInstant(instant, TimeZone.of("Asia/Kolkata")).localDateTime,
        )
    }

    @Test
    fun offsetAccountsForDaylightSaving() {
        val london = TimeZone.of("Europe/London")
        assertEquals(UtcOffset(hours = 1), ZonedInstant(instant, london).offset)
        assertEquals(
            UtcOffset(hours = 0),
            ZonedInstant(Instant.parse("2024-01-01T12:00:00Z"), london).offset,
        )
    }
}
