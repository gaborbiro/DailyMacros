package dev.gaborbiro.dailymacros.features.shared.mealreminders

import dev.gaborbiro.dailymacros.features.common.utils.logicalDiaryDate
import dev.gaborbiro.dailymacros.repositories.common.model.toZonedDateTime
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Decides, for each of breakfast, lunch and dinner, whether and when today's logging reminder
 * should go out. Pure: no clock, storage or Android - the caller passes everything in.
 *
 * Usual meal times are learned from the last [LOOKBACK_DAYS] diary days (only those the
 * [LoggedDayRule] counts), taking the earliest meal-sized record in each slot as that day's meal.
 * Snacks below [MEAL_MIN_CALORIES] are ignored so a mid-morning biscuit doesn't drag breakfast.
 */
object MealReminderPlanner {

    const val LOOKBACK_DAYS = 28L
    const val MEAL_MIN_CALORIES = 250
    const val MIN_SAMPLES = 4
    const val MIN_SPLIT_SAMPLES = 3
    const val TIGHT_SPREAD_MINUTES = 90
    const val CATCH_UP_DELAY_MINUTES = 30L
    const val MIN_GAP_AFTER_MEAL_MINUTES = 3 * 60L
    const val MIN_GAP_AFTER_ANY_LOG_MINUTES = 30L
    const val LATEST_BASE_BEFORE_WINDOW_END_MINUTES = 30
    const val BACK_OFF_AFTER_IGNORED = 3
    val BACK_OFF_DURATION: Duration = Duration.ofDays(7)

    fun plan(
        records: List<Record>,
        now: ZonedDateTime,
        dayStart: LocalTime,
        loggedDayRule: LoggedDayRule,
        history: Map<MealSlot, SlotReminderHistory> = emptyMap(),
    ): List<MealSlotPlan> {
        val logs = records.map { it.toMealLog() }
        val today = now.logicalDiaryDate(dayStart)
        val learned = learnMealTimes(logs, today, dayStart, loggedDayRule)
        return MealSlot.entries.map { slot ->
            MealSlotPlan(
                slot = slot,
                learned = learned.getValue(slot),
                decision = decide(
                    slot = slot,
                    learned = learned.getValue(slot),
                    logs = logs,
                    now = now,
                    today = today,
                    dayStart = dayStart,
                    history = history[slot] ?: SlotReminderHistory(),
                ),
            )
        }
    }

    /**
     * Settles reminders still [SlotReminderHistory.awaitingOutcome]: one whose slot got a meal
     * logged that diary day (even backdated) was acted on and resets the ignored streak; one whose
     * slot ended without a meal counts as ignored. Others stay pending.
     */
    fun resolveOutcomes(
        records: List<Record>,
        now: ZonedDateTime,
        dayStart: LocalTime,
        history: Map<MealSlot, SlotReminderHistory>,
    ): Map<MealSlot, SlotReminderHistory> {
        val logs = records.map { it.toMealLog() }
        return history.mapValues { (slot, slotHistory) ->
            val remindedAt = slotHistory.lastRemindedAt
            if (!slotHistory.awaitingOutcome || remindedAt == null) return@mapValues slotHistory
            val day = remindedAt.logicalDiaryDate(dayStart)
            val logged = logs.any {
                it.mightBeMeal() &&
                    it.timestamp.logicalDiaryDate(dayStart) == day &&
                    slotOf(it.timestamp.diaryMinute(dayStart), dayStart) == slot
            }
            when {
                logged -> slotHistory.copy(consecutiveIgnored = 0, awaitingOutcome = false)
                !now.isBefore(slot.windowEnd(day, dayStart, now)) ->
                    slotHistory.copy(consecutiveIgnored = slotHistory.consecutiveIgnored + 1, awaitingOutcome = false)

                else -> slotHistory
            }
        }
    }

    /** When [slot] ends on diary day [day], in [reference]'s zone. */
    fun MealSlot.windowEnd(day: LocalDate, dayStart: LocalTime, reference: ZonedDateTime): ZonedDateTime =
        day.atStartOfDay().plusMinutes(windowEndMinute(dayStart).toLong()).atZone(reference.zone)

    private fun learnMealTimes(
        logs: List<MealLog>,
        today: LocalDate,
        dayStart: LocalTime,
        loggedDayRule: LoggedDayRule,
    ): Map<MealSlot, LearnedMealTime> {
        val firstDay = today.minusDays(LOOKBACK_DAYS)
        val countedDays = logs
            .groupBy { it.timestamp.logicalDiaryDate(dayStart) }
            .filterKeys { it >= firstDay && it < today }
            .filterValues { loggedDayRule.counts(it) }

        // One sample per (day, slot): when that day's meal in that slot started.
        val samples: Map<MealSlot, List<Pair<LocalDate, Int>>> = countedDays
            .flatMap { (day, dayLogs) ->
                dayLogs
                    .filter { it.isKnownMeal() }
                    .mapNotNull { log ->
                        val minute = log.timestamp.diaryMinute(dayStart)
                        slotOf(minute, dayStart)?.let { slot -> Triple(slot, day, minute) }
                    }
                    .groupBy { it.first }
                    .map { (slot, meals) -> slot to (day to meals.minOf { it.third }) }
            }
            .groupBy({ it.first }, { it.second })

        val todayIsWeekend = today.isWeekend()
        return MealSlot.entries.associateWith { slot ->
            val all = samples[slot].orEmpty()
            if (all.size < MIN_SAMPLES) {
                LearnedMealTime(
                    typicalMinute = slot.defaultTime.toSecondOfDay() / 60,
                    earlyMinute = null,
                    lateMinute = null,
                    sampleCount = all.size,
                    basis = LearnedMealTime.Basis.DEFAULT,
                    consistency = LearnedMealTime.Consistency.UNKNOWN,
                )
            } else {
                val sameKindOfDay = all.filter { it.first.isWeekend() == todayIsWeekend }
                val (minutes, basis) = when {
                    sameKindOfDay.size < MIN_SPLIT_SAMPLES || sameKindOfDay.size == all.size ->
                        all.map { it.second } to LearnedMealTime.Basis.ALL_DAYS

                    todayIsWeekend -> sameKindOfDay.map { it.second } to LearnedMealTime.Basis.WEEKENDS
                    else -> sameKindOfDay.map { it.second } to LearnedMealTime.Basis.WEEKDAYS
                }
                val sorted = minutes.sorted()
                val early = percentile(sorted, 0.25)
                val late = percentile(sorted, 0.75)
                LearnedMealTime(
                    typicalMinute = percentile(sorted, 0.5),
                    earlyMinute = early,
                    lateMinute = late,
                    sampleCount = all.size,
                    basis = basis,
                    consistency = if (late - early <= TIGHT_SPREAD_MINUTES) {
                        LearnedMealTime.Consistency.TIGHT
                    } else {
                        LearnedMealTime.Consistency.LOOSE
                    },
                )
            }
        }
    }

    private fun decide(
        slot: MealSlot,
        learned: LearnedMealTime,
        logs: List<MealLog>,
        now: ZonedDateTime,
        today: LocalDate,
        dayStart: LocalTime,
        history: SlotReminderHistory,
    ): MealReminderDecision {
        fun todayAt(minute: Int): ZonedDateTime =
            today.atStartOfDay().plusMinutes(minute.toLong()).atZone(now.zone)

        val windowEndMinute = slot.windowEndMinute(dayStart)
        val windowEnd = slot.windowEnd(today, dayStart, now)

        val loggedInSlotToday = logs.any {
            it.mightBeMeal() &&
                it.timestamp.logicalDiaryDate(dayStart) == today &&
                slotOf(it.timestamp.diaryMinute(dayStart), dayStart) == slot
        }
        if (loggedInSlotToday) return MealReminderDecision.Silent(SilenceReason.ALREADY_LOGGED)
        if (!now.isBefore(windowEnd)) return MealReminderDecision.Silent(SilenceReason.WINDOW_OVER)

        val lastRemindedAt = history.lastRemindedAt
        if (lastRemindedAt != null && lastRemindedAt.logicalDiaryDate(dayStart) == today) {
            return MealReminderDecision.Silent(SilenceReason.ALREADY_REMINDED)
        }
        if (
            lastRemindedAt != null &&
            history.consecutiveIgnored >= BACK_OFF_AFTER_IGNORED &&
            now.isBefore(lastRemindedAt.plus(BACK_OFF_DURATION))
        ) {
            return MealReminderDecision.Silent(SilenceReason.BACKED_OFF)
        }

        val style = when (learned.consistency) {
            LearnedMealTime.Consistency.LOOSE -> ReminderStyle.CATCH_UP
            LearnedMealTime.Consistency.TIGHT,
            LearnedMealTime.Consistency.UNKNOWN,
                -> ReminderStyle.PHOTO_PROMPT
        }
        val baseMinute = when (style) {
            ReminderStyle.PHOTO_PROMPT -> learned.typicalMinute
            ReminderStyle.CATCH_UP -> learned.lateMinute!! + CATCH_UP_DELAY_MINUTES.toInt()
        }.coerceIn(slot.windowStartMinute, windowEndMinute - LATEST_BASE_BEFORE_WINDOW_END_MINUTES)

        val pastLogs = logs.filter { !it.timestamp.isAfter(now) }
        val afterLastMeal = pastLogs
            .filter { it.mightBeMeal() }
            .maxOfOrNull { it.timestamp }
            ?.plusMinutes(MIN_GAP_AFTER_MEAL_MINUTES)
        val afterLastLog = pastLogs
            .maxOfOrNull { it.timestamp }
            ?.plusMinutes(MIN_GAP_AFTER_ANY_LOG_MINUTES)

        val at = listOfNotNull(todayAt(baseMinute), afterLastMeal, afterLastLog)
            .maxBy { it.toInstant() }
            .withZoneSameInstant(now.zone)
        return if (at.isBefore(windowEnd)) {
            MealReminderDecision.Remind(at = at, style = style)
        } else {
            MealReminderDecision.Silent(SilenceReason.ATE_RECENTLY)
        }
    }

    /** Minutes after the diary date's local midnight; times before [dayStart] belong to the previous day, so run past 24h. */
    internal fun ZonedDateTime.diaryMinute(dayStart: LocalTime): Int {
        val local = toLocalTime()
        val minuteOfDay = local.toSecondOfDay() / 60
        return if (local < dayStart) minuteOfDay + 24 * 60 else minuteOfDay
    }

    internal fun MealSlot.windowEndMinute(dayStart: LocalTime): Int = when (this) {
        MealSlot.BREAKFAST -> MealSlot.LUNCH.windowStartMinute
        MealSlot.LUNCH -> MealSlot.DINNER.windowStartMinute
        MealSlot.DINNER -> 24 * 60 + dayStart.toSecondOfDay() / 60
    }

    internal fun slotOf(diaryMinute: Int, dayStart: LocalTime): MealSlot? =
        MealSlot.entries
            .lastOrNull { diaryMinute >= it.windowStartMinute }
            ?.takeIf { diaryMinute < it.windowEndMinute(dayStart) }

    /** The parts of a [Record] the planner needs, with the timestamp converted to java.time once. */
    private class MealLog(val timestamp: ZonedDateTime, val calories: Int?)

    private fun Record.toMealLog() = MealLog(timestamp.toZonedDateTime(), template.nutrients.calories)

    private fun LoggedDayRule.counts(dayLogs: List<MealLog>): Boolean = when (this) {
        LoggedDayRule.AnyRecord -> dayLogs.isNotEmpty()
        is LoggedDayRule.MinimumCalories -> dayLogs.sumOf { it.calories ?: 0 } >= threshold
    }

    /** Analysed and big enough to be a meal - the only records trusted for learning times. */
    private fun MealLog.isKnownMeal(): Boolean =
        (calories ?: 0) >= MEAL_MIN_CALORIES

    /**
     * For staying quiet, also trust records whose calories are unknown (still analysing, or never
     * analysed): a photo snapped five minutes ago is most likely the meal we'd be reminding about.
     */
    private fun MealLog.mightBeMeal(): Boolean =
        calories.let { it == null || it >= MEAL_MIN_CALORIES }

    private fun LocalDate.isWeekend(): Boolean =
        dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

    /** Linear interpolation between the closest ranks; [sorted] must be non-empty. */
    private fun percentile(sorted: List<Int>, p: Double): Int {
        val position = p * (sorted.size - 1)
        val lower = sorted[floor(position).toInt()]
        val upper = sorted[ceil(position).toInt()]
        return (lower + (upper - lower) * (position - floor(position))).roundToInt()
    }
}
