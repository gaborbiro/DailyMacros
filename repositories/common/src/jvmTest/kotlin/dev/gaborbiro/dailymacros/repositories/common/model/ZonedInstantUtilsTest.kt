package dev.gaborbiro.dailymacros.repositories.common.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class ZonedInstantUtilsTest {

    private val london = ZoneId.of("Europe/London")

    // UK clocks go back at 02:00 BST on 25 Oct 2026, so 01:30 happens twice.
    private val overlapLocal = java.time.LocalDateTime.of(2026, 10, 25, 1, 30)
    private val firstOneThirty = ZonedDateTime.ofLocal(overlapLocal, london, null).withEarlierOffsetAtOverlap()
    private val secondOneThirty = firstOneThirty.withLaterOffsetAtOverlap()

    @Test
    fun `ZonedDateTime round-trips through ZonedInstant unchanged`() {
        val original = ZonedDateTime.of(2024, 5, 10, 14, 7, 30, 123_000_000, ZoneId.of("Asia/Kolkata"))

        assertEquals(original, original.toZonedInstant().toZonedDateTime())
    }

    @Test
    fun `ZonedInstant round-trips through ZonedDateTime unchanged`() {
        val original = ZonedInstant(Instant.parse("2024-05-10T08:37:30.123Z"), TimeZone.of("Asia/Kolkata"))

        assertEquals(original, original.toZonedDateTime().toZonedInstant())
    }

    @Test
    fun `toZonedInstant keeps the instant and the zone id`() {
        val original = ZonedDateTime.of(2024, 5, 10, 14, 7, 0, 0, london)

        val zoned = original.toZonedInstant()

        assertEquals(original.toInstant().toEpochMilli(), zoned.instant.toEpochMilliseconds())
        assertEquals("Europe/London", zoned.timeZone.id)
    }

    @Test
    fun `both occurrences of an ambiguous local time stay distinct`() {
        val first = firstOneThirty.toZonedInstant()
        val second = secondOneThirty.toZonedInstant()

        assertNotEquals(first, second)
        assertEquals(1.hours, second.instant - first.instant)
        assertEquals(LocalDateTime(2026, 10, 25, 1, 30), first.localDateTime)
        assertEquals(LocalDateTime(2026, 10, 25, 1, 30), second.localDateTime)
    }

    @Test
    fun `both occurrences of an ambiguous local time round-trip with their original offset`() {
        val first = firstOneThirty.toZonedInstant().toZonedDateTime()
        val second = secondOneThirty.toZonedInstant().toZonedDateTime()

        assertEquals(firstOneThirty, first)
        assertEquals(ZoneOffset.ofHours(1), first.offset)
        assertEquals(secondOneThirty, second)
        assertEquals(ZoneOffset.UTC, second.offset)
    }

    @Test
    fun `local time skipped by spring-forward keeps java's resolution`() {
        // UK clocks jump from 01:00 to 02:00 on 29 Mar 2026; java resolves 01:30 to 02:30 BST.
        val resolved = ZonedDateTime.of(2026, 3, 29, 1, 30, 0, 0, london)

        val zoned = resolved.toZonedInstant()

        assertEquals(LocalDateTime(2026, 3, 29, 2, 30), zoned.localDateTime)
        assertEquals(resolved, zoned.toZonedDateTime())
    }

    @Test
    fun `fixed-offset zone round-trips`() {
        val original = ZonedDateTime.of(2024, 5, 10, 14, 7, 0, 0, ZoneOffset.ofHoursMinutes(5, 30))

        assertEquals(original, original.toZonedInstant().toZonedDateTime())
    }
}
