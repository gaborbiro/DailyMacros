package dev.gaborbiro.dailymacros.repositories.records.domain.model

import dev.gaborbiro.dailymacros.repositories.common.model.ZonedInstant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

// Runs on the JVM and on the iOS simulator, so it also checks that both platforms' time zone
// databases agree on these offsets.
class TimezoneShiftTest {

    private fun at(hour: Int, zoneId: String): ZonedInstant {
        val zone = TimeZone.of(zoneId)
        return ZonedInstant(LocalDateTime(2024, 3, 15, hour, 0).toInstant(zone), zone)
    }

    @Test
    fun sameZone_noShift() {
        assertNull(timezoneShift(at(8, "Europe/Lisbon"), at(14, "Europe/Lisbon")))
    }

    @Test
    fun eastwardFlight_positiveShift() {
        assertEquals(4.hours, timezoneShift(at(10, "Europe/Lisbon"), at(14, "Asia/Dubai")))
    }

    @Test
    fun westwardFlight_negativeShiftWithMinutes() {
        assertEquals(
            -(5.hours + 30.minutes),
            timezoneShift(at(10, "Asia/Kolkata"), at(14, "Europe/Lisbon")),
        )
    }

    @Test
    fun differentZonesWithSameOffset_noShift() {
        // Both UTC+0 in mid-March (before either switches to summer time).
        assertNull(timezoneShift(at(8, "Europe/Lisbon"), at(9, "Europe/London")))
    }
}
