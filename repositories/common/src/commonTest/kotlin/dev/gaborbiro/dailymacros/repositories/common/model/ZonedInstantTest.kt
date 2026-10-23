package dev.gaborbiro.dailymacros.repositories.common.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Instant

class ZonedInstantTest {

    @Test
    fun localDateTime_returnsCorrectLocalDateTimeInUtc() {
        val instant = Instant.parse("2024-03-15T12:30:45Z")
        val zonedInstant = ZonedInstant(instant, TimeZone.UTC)

        val expected = LocalDateTime(2024, 3, 15, 12, 30, 45)
        assertEquals(expected, zonedInstant.localDateTime)
    }

    @Test
    fun localDateTime_convertsToPositiveOffsetTimeZone() {
        val instant = Instant.parse("2024-03-15T12:00:00Z")
        val timeZone = TimeZone.of("Asia/Tokyo") // UTC+9
        val zonedInstant = ZonedInstant(instant, timeZone)

        val expected = LocalDateTime(2024, 3, 15, 21, 0, 0)
        assertEquals(expected, zonedInstant.localDateTime)
    }

    @Test
    fun localDateTime_convertsToNegativeOffsetTimeZone() {
        val instant = Instant.parse("2024-03-15T12:00:00Z")
        val timeZone = TimeZone.of("America/New_York") // UTC-4 in EDT
        val zonedInstant = ZonedInstant(instant, timeZone)

        val expected = LocalDateTime(2024, 3, 15, 8, 0, 0)
        assertEquals(expected, zonedInstant.localDateTime)
    }

    @Test
    fun localDateTime_handlesDayBoundaryCross() {
        val instant = Instant.parse("2024-03-15T23:30:00Z")
        val timeZone = TimeZone.of("Asia/Tokyo") // UTC+9 -> 2024-03-16 08:30:00
        val zonedInstant = ZonedInstant(instant, timeZone)

        val expected = LocalDateTime(2024, 3, 16, 8, 30, 0)
        assertEquals(expected, zonedInstant.localDateTime)
    }

    @Test
    fun localDateTime_handlesLeapYear() {
        val instant = Instant.parse("2024-02-29T15:45:00Z")
        val zonedInstant = ZonedInstant(instant, TimeZone.UTC)

        val expected = LocalDateTime(2024, 2, 29, 15, 45, 0)
        assertEquals(expected, zonedInstant.localDateTime)
    }

    @Test
    fun localDateTime_isAmbiguousDuringFallBackButZonedInstantsStayDistinct() {
        // UK clocks go back at 02:00 BST on 25 Oct 2026, so 01:30 local happens twice.
        val london = TimeZone.of("Europe/London")
        val firstOneThirty = ZonedInstant(Instant.parse("2026-10-25T00:30:00Z"), london) // BST, UTC+1
        val secondOneThirty = ZonedInstant(Instant.parse("2026-10-25T01:30:00Z"), london) // GMT, UTC+0

        val expected = LocalDateTime(2026, 10, 25, 1, 30, 0)
        assertEquals(expected, firstOneThirty.localDateTime)
        assertEquals(expected, secondOneThirty.localDateTime)
        assertNotEquals(firstOneThirty, secondOneThirty)
    }

    @Test
    fun sameInstantAndZone_isEqual() {
        val instant = Instant.parse("2024-01-01T00:00:00Z")

        assertEquals(ZonedInstant(instant, TimeZone.UTC), ZonedInstant(instant, TimeZone.UTC))
    }

    @Test
    fun differentInstantInSameZone_isNotEqual() {
        val zone = TimeZone.UTC

        assertNotEquals(
            ZonedInstant(Instant.parse("2024-01-01T00:00:00Z"), zone),
            ZonedInstant(Instant.parse("2024-01-01T12:00:00Z"), zone),
        )
    }

    @Test
    fun sameInstantAndOffsetInDifferentZones_isNotEqual() {
        // On 1 Jan London is on GMT, so both have offset +0 and show the same local time.
        val instant = Instant.parse("2024-01-01T00:00:00Z")

        assertNotEquals(
            ZonedInstant(instant, TimeZone.UTC),
            ZonedInstant(instant, TimeZone.of("Europe/London")),
        )
    }
}
