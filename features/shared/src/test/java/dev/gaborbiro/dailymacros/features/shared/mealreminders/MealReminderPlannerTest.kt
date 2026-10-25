package dev.gaborbiro.dailymacros.features.shared.mealreminders

import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.common.model.TopContributors
import dev.gaborbiro.dailymacros.repositories.common.model.toZonedInstant
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class MealReminderPlannerTest {

    private val zone = ZoneId.of("Europe/London")

    /** A Wednesday, in winter so London is on UTC+0. */
    private val today = LocalDate.of(2024, 2, 7)

    private var nextId = 1L

    private fun record(date: LocalDate, time: String, calories: Int?): Record {
        val id = nextId++
        return Record(
            recordId = id,
            timestamp = ZonedDateTime.of(date, LocalTime.parse(time), zone).toZonedInstant(),
            template = Template(
                dbId = id,
                imageFilenames = emptyList(),
                isRepresentativeOfMealByImageIndex = emptyList(),
                name = "Meal",
                description = "",
                isPending = calories == null,
                nutrients = Nutrients(calories = calories),
                notes = "",
                mealComponents = emptyList(),
                topContributors = TopContributors(),
                quickPickOverride = null,
            ),
        )
    }

    private fun at(time: String, date: LocalDate = today): ZonedDateTime =
        ZonedDateTime.of(date, LocalTime.parse(time), zone)

    /**
     * One meal per past weekday, the first on or before [startDaysBack] (1 = yesterday) and going
     * further back. Weekdays only, so the weekday/weekend split doesn't kick in.
     */
    private fun history(vararg times: String, calories: Int = 500, startDaysBack: Long = 1): List<Record> {
        val weekdays = generateSequence(today.minusDays(startDaysBack)) { it.minusDays(1) }
            .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
        return times.zip(weekdays.take(times.size).toList()) { time, day -> record(day, time, calories) }
    }

    private fun plan(
        records: List<Record>,
        now: ZonedDateTime = at("06:00"),
        dayStart: LocalTime = LocalTime.MIDNIGHT,
        rule: LoggedDayRule = LoggedDayRule.AnyRecord,
        history: Map<MealSlot, SlotReminderHistory> = emptyMap(),
    ): Map<MealSlot, MealSlotPlan> =
        MealReminderPlanner.plan(records, now, dayStart, rule, history).associateBy { it.slot }

    private fun remind(time: String, style: ReminderStyle = ReminderStyle.PHOTO_PROMPT) =
        MealReminderDecision.Remind(at(time), style)

    private fun silent(reason: SilenceReason) = MealReminderDecision.Silent(reason)

    @Test
    fun `without history every slot uses its default time and asks for a photo`() {
        val plans = plan(records = emptyList())

        assertEquals(remind("08:00"), plans.getValue(MealSlot.BREAKFAST).decision)
        assertEquals(remind("12:30"), plans.getValue(MealSlot.LUNCH).decision)
        assertEquals(remind("19:00"), plans.getValue(MealSlot.DINNER).decision)
        assertEquals(LearnedMealTime.Basis.DEFAULT, plans.getValue(MealSlot.LUNCH).learned.basis)
    }

    @Test
    fun `three meals are not enough to learn from`() {
        val plans = plan(history("13:30", "13:30", "13:30"))

        assertEquals(LearnedMealTime.Basis.DEFAULT, plans.getValue(MealSlot.LUNCH).learned.basis)
        assertEquals(3, plans.getValue(MealSlot.LUNCH).learned.sampleCount)
        assertEquals(remind("12:30"), plans.getValue(MealSlot.LUNCH).decision)
    }

    @Test
    fun `consistent breakfast reminds at the median time with a photo prompt`() {
        val plans = plan(history("08:10", "08:30", "08:40", "08:50", "09:20"))

        val breakfast = plans.getValue(MealSlot.BREAKFAST)
        assertEquals(LearnedMealTime.Consistency.TIGHT, breakfast.learned.consistency)
        assertEquals(remind("08:40"), breakfast.decision)
    }

    @Test
    fun `erratic lunch gets a catch-up after the usual latest time`() {
        // Sorted: 11:30, 12:00, 13:00, 14:30, 15:30 -> 25th pct 12:00, 75th pct 14:30 (spread 150 min).
        val plans = plan(history("12:00", "14:30", "11:30", "15:30", "13:00"))

        val lunch = plans.getValue(MealSlot.LUNCH)
        assertEquals(LearnedMealTime.Consistency.LOOSE, lunch.learned.consistency)
        assertEquals(remind("15:00", ReminderStyle.CATCH_UP), lunch.decision)
    }

    @Test
    fun `catch-up time stays inside the slot`() {
        // 75th pct 15:50 + 30 min would be 16:20, past lunch; clamped to 30 min before the slot ends.
        val plans = plan(history("11:10", "11:20", "15:50", "15:50", "15:55"))

        assertEquals(remind("15:30", ReminderStyle.CATCH_UP), plans.getValue(MealSlot.LUNCH).decision)
    }

    @Test
    fun `snacks are not mistaken for meals when learning`() {
        val snacks = history("10:30", "10:30", "10:30", "10:30", calories = 150)
        val breakfasts = history("07:30", "07:30", "07:30", "07:30")

        val breakfast = plan(snacks + breakfasts).getValue(MealSlot.BREAKFAST)

        assertEquals(7 * 60 + 30, breakfast.learned.typicalMinute)
    }

    @Test
    fun `earliest meal-sized record of the day is the meal`() {
        val first = history("12:00", "12:00", "12:00", "12:00")
        val seconds = history("14:00", "14:00", "14:00", "14:00")

        assertEquals(12 * 60, plan(first + seconds).getValue(MealSlot.LUNCH).learned.typicalMinute)
    }

    @Test
    fun `days outside the lookback are ignored`() {
        val old = history("09:30", "09:30", "09:30", "09:30", startDaysBack = 29)

        assertEquals(LearnedMealTime.Basis.DEFAULT, plan(old).getValue(MealSlot.BREAKFAST).learned.basis)
    }

    @Test
    fun `minimum calorie rule only learns from days that reached the threshold`() {
        // Four well-logged days with 13:00 lunch, four half-logged days with 12:00 lunch.
        val full = (1L..4L).flatMap { daysBack ->
            val day = today.minusDays(daysBack)
            listOf(record(day, "13:00", 700), record(day, "19:00", 900))
        }
        val partial = history("12:00", "12:00", "12:00", "12:00", startDaysBack = 5)

        val anyRecord = plan(full + partial).getValue(MealSlot.LUNCH).learned
        val qualified = plan(full + partial, rule = LoggedDayRule.MinimumCalories(1200)).getValue(MealSlot.LUNCH).learned

        assertEquals(8, anyRecord.sampleCount)
        assertEquals(4, qualified.sampleCount)
        assertEquals(13 * 60, qualified.typicalMinute)
    }

    @Test
    fun `weekdays and weekends are learned separately once each has enough meals`() {
        val past = (1L..28L).map { today.minusDays(it) }
        val records = past.map { day ->
            val weekend = day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY
            record(day, if (weekend) "10:30" else "07:30", 400)
        }

        val weekday = plan(records).getValue(MealSlot.BREAKFAST).learned
        val saturday = today.plusDays(3)
        val weekend = plan(records, now = at("06:00", saturday)).getValue(MealSlot.BREAKFAST).learned

        assertEquals(LearnedMealTime.Basis.WEEKDAYS, weekday.basis)
        assertEquals(7 * 60 + 30, weekday.typicalMinute)
        assertEquals(LearnedMealTime.Basis.WEEKENDS, weekend.basis)
        assertEquals(10 * 60 + 30, weekend.typicalMinute)
    }

    @Test
    fun `too few weekend meals fall back to all days`() {
        val saturday = today.plusDays(3)
        // Weekday-only history: none of these is a weekend day.
        val weekdays = (1L..10L).map { today.minusDays(it) }
            .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
            .map { record(it, "07:30", 400) }

        val learned = plan(weekdays, now = at("06:00", saturday)).getValue(MealSlot.BREAKFAST).learned

        assertEquals(LearnedMealTime.Basis.ALL_DAYS, learned.basis)
    }

    @Test
    fun `slot already logged today stays silent`() {
        val plans = plan(listOf(record(today, "12:45", 600)), now = at("13:00"))

        assertEquals(silent(SilenceReason.ALREADY_LOGGED), plans.getValue(MealSlot.LUNCH).decision)
    }

    @Test
    fun `a photo still being analysed counts as logged`() {
        val plans = plan(listOf(record(today, "12:45", calories = null)), now = at("12:50"))

        assertEquals(silent(SilenceReason.ALREADY_LOGGED), plans.getValue(MealSlot.LUNCH).decision)
    }

    @Test
    fun `a snack in the slot does not count as the meal but delays the reminder`() {
        val plans = plan(listOf(record(today, "12:20", 120)), now = at("12:25"))

        assertEquals(remind("12:50"), plans.getValue(MealSlot.LUNCH).decision)
    }

    @Test
    fun `late breakfast pushes lunch back three hours`() {
        val plans = plan(listOf(record(today, "10:45", 600)), now = at("10:50"))

        assertEquals(silent(SilenceReason.ALREADY_LOGGED), plans.getValue(MealSlot.BREAKFAST).decision)
        assertEquals(remind("13:45"), plans.getValue(MealSlot.LUNCH).decision)
    }

    @Test
    fun `lunch eaten late in its slot pushes a learned early dinner back`() {
        val earlyDinners = history("16:30", "16:30", "16:30", "16:30")
        val lunch = record(today, "15:55", 700)

        val plans = plan(earlyDinners + lunch, now = at("16:00"))

        assertEquals(remind("18:55"), plans.getValue(MealSlot.DINNER).decision)
    }

    @Test
    fun `a log pushing the reminder past the end of its slot silences it`() {
        // The 15:40 snack delays lunch to 16:10, after the slot ends at 16:00.
        val plans = plan(listOf(record(today, "15:40", 100)), now = at("15:45"))

        assertEquals(silent(SilenceReason.ATE_RECENTLY), plans.getValue(MealSlot.LUNCH).decision)
    }

    @Test
    fun `slot that is over today is silent`() {
        val plans = plan(emptyList(), now = at("16:30"))

        assertEquals(silent(SilenceReason.WINDOW_OVER), plans.getValue(MealSlot.BREAKFAST).decision)
        assertEquals(silent(SilenceReason.WINDOW_OVER), plans.getValue(MealSlot.LUNCH).decision)
        assertEquals(remind("19:00"), plans.getValue(MealSlot.DINNER).decision)
    }

    @Test
    fun `reminder time that already passed is still returned while the slot is open`() {
        val plans = plan(emptyList(), now = at("13:15"))

        assertEquals(remind("12:30"), plans.getValue(MealSlot.LUNCH).decision)
    }

    @Test
    fun `only one reminder per slot per day`() {
        val history = mapOf(MealSlot.LUNCH to SlotReminderHistory(lastRemindedAt = at("12:30")))

        val plans = plan(emptyList(), now = at("13:00"), history = history)

        assertEquals(silent(SilenceReason.ALREADY_REMINDED), plans.getValue(MealSlot.LUNCH).decision)
    }

    @Test
    fun `three ignored reminders in a row pause the slot for a week`() {
        val lastWeek = today.minusDays(6)
        val ignoredThrice = mapOf(
            MealSlot.LUNCH to SlotReminderHistory(lastRemindedAt = at("12:30", lastWeek), consecutiveIgnored = 3),
        )
        val ignoredTwice = mapOf(
            MealSlot.LUNCH to SlotReminderHistory(lastRemindedAt = at("12:30", lastWeek), consecutiveIgnored = 2),
        )
        val pauseOver = mapOf(
            MealSlot.LUNCH to SlotReminderHistory(lastRemindedAt = at("12:30", today.minusDays(8)), consecutiveIgnored = 3),
        )

        assertEquals(silent(SilenceReason.BACKED_OFF), plan(emptyList(), history = ignoredThrice).getValue(MealSlot.LUNCH).decision)
        assertEquals(remind("12:30"), plan(emptyList(), history = ignoredTwice).getValue(MealSlot.LUNCH).decision)
        assertEquals(remind("12:30"), plan(emptyList(), history = pauseOver).getValue(MealSlot.LUNCH).decision)
    }

    @Test
    fun `with a 2am day start a meal after midnight is the previous day's dinner`() {
        val twoAm = LocalTime.of(2, 0)
        // Each 00:30 meal belongs to the diary day before its calendar date.
        val lateDinners = (0L..3L).map { record(today.minusDays(it), "00:30", 800) }

        val dinner = plan(lateDinners, now = at("06:00"), dayStart = twoAm).getValue(MealSlot.DINNER)

        assertEquals(4, dinner.learned.sampleCount)
        assertEquals(24 * 60 + 30, dinner.learned.typicalMinute)
        assertEquals(MealReminderDecision.Remind(at("00:30", today.plusDays(1)), ReminderStyle.PHOTO_PROMPT), dinner.decision)
    }

    private fun resolve(
        records: List<Record>,
        now: ZonedDateTime,
        slotHistory: SlotReminderHistory,
    ): SlotReminderHistory =
        MealReminderPlanner.resolveOutcomes(records, now, LocalTime.MIDNIGHT, mapOf(MealSlot.LUNCH to slotHistory))
            .getValue(MealSlot.LUNCH)

    @Test
    fun `logging the meal after a reminder resets the ignored streak`() {
        val reminded = SlotReminderHistory(lastRemindedAt = at("12:30"), consecutiveIgnored = 2, awaitingOutcome = true)

        val resolved = resolve(listOf(record(today, "12:50", 600)), now = at("13:00"), reminded)

        assertEquals(SlotReminderHistory(lastRemindedAt = at("12:30"), consecutiveIgnored = 0), resolved)
    }

    @Test
    fun `a meal logged later but backdated into the slot still counts as acted on`() {
        val reminded = SlotReminderHistory(lastRemindedAt = at("12:30"), consecutiveIgnored = 1, awaitingOutcome = true)

        val resolved = resolve(listOf(record(today, "12:10", 600)), now = at("21:00"), reminded)

        assertEquals(0, resolved.consecutiveIgnored)
        assertEquals(false, resolved.awaitingOutcome)
    }

    @Test
    fun `a reminder whose slot ended without a meal counts as ignored`() {
        val reminded = SlotReminderHistory(lastRemindedAt = at("12:30"), consecutiveIgnored = 2, awaitingOutcome = true)

        val resolved = resolve(listOf(record(today, "15:00", 100)), now = at("16:00"), reminded)

        assertEquals(3, resolved.consecutiveIgnored)
        assertEquals(false, resolved.awaitingOutcome)
    }

    @Test
    fun `a reminder stays pending while its slot is still open`() {
        val reminded = SlotReminderHistory(lastRemindedAt = at("12:30"), consecutiveIgnored = 1, awaitingOutcome = true)

        assertEquals(reminded, resolve(emptyList(), now = at("15:59"), reminded))
    }

    @Test
    fun `a settled reminder is left alone`() {
        val settled = SlotReminderHistory(lastRemindedAt = at("12:30", today.minusDays(1)), consecutiveIgnored = 1)

        assertEquals(settled, resolve(emptyList(), now = at("20:00"), settled))
    }

    @Test
    fun `a meal on another day does not settle yesterday's reminder as acted on`() {
        val yesterday = today.minusDays(1)
        val reminded = SlotReminderHistory(lastRemindedAt = at("12:30", yesterday), awaitingOutcome = true)

        val resolved = resolve(listOf(record(today, "12:30", 600)), now = at("13:00"), reminded)

        assertEquals(1, resolved.consecutiveIgnored)
    }

    @Test
    fun `a meal moved from the evening to lunchtime no longer silences dinner`() {
        // Erratic dinners: 75th percentile 21:00, so the catch-up goes out at 21:30.
        val dinners = history("18:00", "21:00", "19:00", "21:00", "18:30")
        val loggedAtDinner = record(today, "19:30", 600)
        val movedToLunch = loggedAtDinner.copy(timestamp = at("13:00").toZonedInstant())

        val before = plan(dinners + loggedAtDinner, now = at("19:35")).getValue(MealSlot.DINNER)
        val after = plan(dinners + movedToLunch, now = at("19:36"))

        assertEquals(silent(SilenceReason.ALREADY_LOGGED), before.decision)
        assertEquals(silent(SilenceReason.ALREADY_LOGGED), after.getValue(MealSlot.LUNCH).decision)
        assertEquals(remind("21:30", ReminderStyle.CATCH_UP), after.getValue(MealSlot.DINNER).decision)
    }
}
