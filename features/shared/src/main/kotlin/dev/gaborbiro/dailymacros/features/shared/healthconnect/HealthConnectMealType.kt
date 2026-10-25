package dev.gaborbiro.dailymacros.features.shared.healthconnect

import androidx.health.connect.client.records.MealType
import dev.gaborbiro.dailymacros.features.common.utils.logicalDiaryDate
import dev.gaborbiro.dailymacros.features.shared.mealreminders.MealSlot
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Health Connect's meal type for a meal logged at [timestamp], using the meal reminder windows
 * ([MealSlot.windowStartMinute]). Same day semantics as the reminders: dinner runs until the
 * diary day ends at [dayStart], so with a 2am day start a 00:30 meal is still dinner, and
 * anything before the breakfast window on its diary date is a snack.
 */
internal fun healthConnectMealType(timestamp: ZonedDateTime, dayStart: LocalTime): Int {
    val diaryDate = timestamp.logicalDiaryDate(dayStart)
    // Wall-clock minutes after the diary date's midnight; past 24h for times before dayStart.
    val diaryMinute = ChronoUnit.MINUTES.between(diaryDate.atStartOfDay(), timestamp.toLocalDateTime())
    return when (MealSlot.entries.lastOrNull { diaryMinute >= it.windowStartMinute }) {
        MealSlot.BREAKFAST -> MealType.MEAL_TYPE_BREAKFAST
        MealSlot.LUNCH -> MealType.MEAL_TYPE_LUNCH
        MealSlot.DINNER -> MealType.MEAL_TYPE_DINNER
        null -> MealType.MEAL_TYPE_SNACK
    }
}
