package dev.gaborbiro.dailymacros.features.shared.healthconnect

import androidx.health.connect.client.records.MealType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class HealthConnectMealTypeTest {

    private val midnight = LocalTime.MIDNIGHT

    private fun mealTypeAt(hour: Int, minute: Int, dayStart: LocalTime = midnight) = healthConnectMealType(
        timestamp = ZonedDateTime.of(2024, 3, 15, hour, minute, 0, 0, ZoneId.of("Europe/London")),
        dayStart = dayStart,
    )

    @Test
    fun `just before the breakfast window is a snack`() {
        assertEquals(MealType.MEAL_TYPE_SNACK, mealTypeAt(4, 59))
    }

    @Test
    fun `breakfast window starts at 05_00`() {
        assertEquals(MealType.MEAL_TYPE_BREAKFAST, mealTypeAt(5, 0))
    }

    @Test
    fun `10_59 is still breakfast`() {
        assertEquals(MealType.MEAL_TYPE_BREAKFAST, mealTypeAt(10, 59))
    }

    @Test
    fun `lunch window starts at 11_00`() {
        assertEquals(MealType.MEAL_TYPE_LUNCH, mealTypeAt(11, 0))
    }

    @Test
    fun `15_59 is still lunch`() {
        assertEquals(MealType.MEAL_TYPE_LUNCH, mealTypeAt(15, 59))
    }

    @Test
    fun `dinner window starts at 16_00`() {
        assertEquals(MealType.MEAL_TYPE_DINNER, mealTypeAt(16, 0))
    }

    @Test
    fun `23_59 is dinner`() {
        assertEquals(MealType.MEAL_TYPE_DINNER, mealTypeAt(23, 59))
    }

    @Test
    fun `00_30 is dinner when the diary day starts at 2am`() {
        assertEquals(MealType.MEAL_TYPE_DINNER, mealTypeAt(0, 30, dayStart = LocalTime.of(2, 0)))
    }

    @Test
    fun `00_30 is a snack when the diary day starts at midnight`() {
        assertEquals(MealType.MEAL_TYPE_SNACK, mealTypeAt(0, 30))
    }

    @Test
    fun `02_30 is a snack when the diary day starts at 2am`() {
        assertEquals(MealType.MEAL_TYPE_SNACK, mealTypeAt(2, 30, dayStart = LocalTime.of(2, 0)))
    }
}
