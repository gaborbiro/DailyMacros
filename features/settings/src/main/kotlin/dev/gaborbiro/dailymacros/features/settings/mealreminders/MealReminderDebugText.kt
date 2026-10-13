package dev.gaborbiro.dailymacros.features.settings.mealreminders

import dev.gaborbiro.dailymacros.features.shared.mealreminders.LearnedMealTime
import dev.gaborbiro.dailymacros.features.shared.mealreminders.LoggedDayRule
import dev.gaborbiro.dailymacros.features.shared.mealreminders.MealReminderDecision
import dev.gaborbiro.dailymacros.features.shared.mealreminders.MealReminderPlan
import dev.gaborbiro.dailymacros.features.shared.mealreminders.MealReminderPlanner
import dev.gaborbiro.dailymacros.features.shared.mealreminders.MealReminderRunner
import dev.gaborbiro.dailymacros.features.shared.mealreminders.MealReminderSchedule.isStale
import dev.gaborbiro.dailymacros.features.shared.mealreminders.MealSlotPlan
import dev.gaborbiro.dailymacros.features.shared.mealreminders.ReminderStyle
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Plain-text readout of [MealReminderPlan] for Settings > Debug, so the learned meal times can be
 * sanity-checked against reality before any reminder is posted. Debug-only, hence not localised.
 */
internal fun MealReminderPlan.toDebugText(now: ZonedDateTime, status: MealReminderRunner.Status): String = buildString {
    appendLine(
        "Reminders: ${if (status.enabled) "on" else "OFF"}, " +
            "notifications ${if (status.notificationsAllowed) "allowed" else "BLOCKED"}"
    )
    status.lastPass?.let { pass ->
        appendLine("Last pass ${pass.at.format(TIME_FORMAT)} (${pass.summary})")
        appendLine("Next wake-up: ${pass.nextWakeUp?.format(DATE_TIME_FORMAT) ?: "none"}")
    } ?: appendLine("Last pass: never")
    appendLine(

        when (val rule = loggedDayRule) {
            LoggedDayRule.AnyRecord -> "Learning from: days with any record"
            is LoggedDayRule.MinimumCalories -> "Learning from: days with ≥${rule.threshold} kcal"
        }
    )
    slots.forEach { appendLine(); append(it.toDebugText(now)) }
}

private fun MealSlotPlan.toDebugText(now: ZonedDateTime): String {
    val name = slot.name.lowercase().replaceFirstChar { it.uppercase() }
    val usual = with(learned) {
        if (basis == LearnedMealTime.Basis.DEFAULT) {
            "default ${typicalMinute.hhmm()} ($sampleCount of ${MealReminderPlanner.MIN_SAMPLES} meals needed)"
        } else {
            val consistency = consistency.name.lowercase()
            val days = basis.name.lowercase().replace('_', ' ')
            "usual ${typicalMinute.hhmm()} (${earlyMinute?.hhmm()}–${lateMinute?.hhmm()}, $consistency, " +
                "$days, $sampleCount meals)"
        }
    }
    val today = when (val decision = decision) {
        is MealReminderDecision.Remind -> {
            val style = when (decision.style) {
                ReminderStyle.PHOTO_PROMPT -> "photo prompt"
                ReminderStyle.CATCH_UP -> "catch-up"
            }
            val due = when {
                decision.at.isAfter(now) -> ""
                decision.isStale(now) -> " (missed, too late to post)"
                else -> " (due now)"
            }
            "$style at ${decision.at.format(TIME_FORMAT)}$due"
        }

        is MealReminderDecision.Silent -> "silent: ${decision.reason.name.lowercase().replace('_', ' ')}"
    }
    return "$name: $usual\n  → $today"
}

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("EEE HH:mm")

private fun Int.hhmm(): String = "%02d:%02d".format((this / 60) % 24, this % 60)
