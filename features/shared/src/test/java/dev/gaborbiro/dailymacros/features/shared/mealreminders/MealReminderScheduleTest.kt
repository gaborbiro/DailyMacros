package dev.gaborbiro.dailymacros.features.shared.mealreminders

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class MealReminderScheduleTest {

    private val zone = ZoneId.of("Europe/London")
    private val today = LocalDate.of(2024, 2, 7)

    private fun at(time: String, date: LocalDate = today): ZonedDateTime =
        ZonedDateTime.of(date, LocalTime.parse(time), zone)

    private val learned = LearnedMealTime(
        typicalMinute = 0,
        earlyMinute = null,
        lateMinute = null,
        sampleCount = 0,
        basis = LearnedMealTime.Basis.DEFAULT,
        consistency = LearnedMealTime.Consistency.UNKNOWN,
    )

    private fun remind(slot: MealSlot, time: String) =
        MealSlotPlan(slot, learned, MealReminderDecision.Remind(at(time), ReminderStyle.PHOTO_PROMPT))

    private fun silent(slot: MealSlot) =
        MealSlotPlan(slot, learned, MealReminderDecision.Silent(SilenceReason.ALREADY_LOGGED))

    private val plans = listOf(
        remind(MealSlot.BREAKFAST, "08:00"),
        remind(MealSlot.LUNCH, "12:30"),
        remind(MealSlot.DINNER, "19:00"),
    )

    @Test
    fun `a photo prompt is due from its time until an hour later`() {
        assertEquals(emptyList<MealSlot>(), MealReminderSchedule.due(plans, at("12:28")).map { it.first })
        assertEquals(listOf(MealSlot.LUNCH), MealReminderSchedule.due(plans, at("12:29:30")).map { it.first })
        assertEquals(listOf(MealSlot.LUNCH), MealReminderSchedule.due(plans, at("13:30")).map { it.first })
        assertEquals(emptyList<MealSlot>(), MealReminderSchedule.due(plans, at("13:31")).map { it.first })
    }

    @Test
    fun `a late catch-up is still due`() {
        val catchUp = MealSlotPlan(
            MealSlot.LUNCH,
            learned,
            MealReminderDecision.Remind(at("12:30"), ReminderStyle.CATCH_UP),
        )

        assertEquals(listOf(MealSlot.LUNCH), MealReminderSchedule.due(listOf(catchUp), at("15:15")).map { it.first })
    }

    @Test
    fun `silent slots are never due`() {
        assertEquals(emptyList<MealSlot>(), MealReminderSchedule.due(listOf(silent(MealSlot.LUNCH)), at("12:30")))
    }

    @Test
    fun `next check is the next upcoming reminder`() {
        assertEquals(at("12:30"), MealReminderSchedule.nextCheck(plans, at("09:00"), LocalTime.MIDNIGHT))
    }

    @Test
    fun `just posted and stale reminders do not schedule an immediate re-run`() {
        val next = MealReminderSchedule.nextCheck(plans, at("12:30"), LocalTime.MIDNIGHT, excluding = setOf(MealSlot.LUNCH))

        assertEquals(at("19:00"), next)
    }

    @Test
    fun `after the last reminder the next check is the start of the next diary day`() {
        assertEquals(at("00:00", today.plusDays(1)), MealReminderSchedule.nextCheck(plans, at("19:30"), LocalTime.MIDNIGHT))
        assertEquals(at("02:00", today.plusDays(1)), MealReminderSchedule.nextCheck(plans, at("19:30"), LocalTime.of(2, 0)))
    }
}
