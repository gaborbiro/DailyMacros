package dev.gaborbiro.dailymacros.repositories.common.model

import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ZonedInstantJvmTest {

    @Test
    fun roundTripKeepsNanosAndRegionZone() {
        val original = ZonedDateTime.of(2024, 3, 31, 1, 30, 15, 123_456_789, ZoneId.of("Europe/Lisbon"))
        assertEquals(original, original.toZonedInstant().toZonedDateTime())
    }

    @Test
    fun roundTripKeepsFixedOffsetZone() {
        val original = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHoursMinutes(5, 45))
        assertEquals(original, original.toZonedInstant().toZonedDateTime())
    }
}
