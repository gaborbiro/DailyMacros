package dev.gaborbiro.dailymacros.features.shared.mealreminders

import dev.gaborbiro.dailymacros.features.common.utils.diaryDayStartTime
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import java.time.ZonedDateTime
import javax.inject.Inject

/** Loads the history [MealReminderPlanner] needs and returns today's plan for each meal slot. */
class GetMealReminderPlanUseCase @Inject constructor(
    private val recordsRepository: RecordsRepository,
    private val settingsRepository: SettingsRepository,
    private val loggedDayRuleProvider: LoggedDayRuleProvider,
) {

    suspend fun execute(
        now: ZonedDateTime = ZonedDateTime.now(),
        history: Map<MealSlot, SlotReminderHistory> = emptyMap(),
    ): MealReminderPlan {
        // Two spare days so the lookback still covers whole diary days around the day-start shift.
        val records = recordsRepository.getRecords(
            since = now.minusDays(MealReminderPlanner.LOOKBACK_DAYS + 2),
        )
        val loggedDayRule = loggedDayRuleProvider.get()
        return MealReminderPlan(
            loggedDayRule = loggedDayRule,
            slots = MealReminderPlanner.plan(
                records = records,
                now = now,
                dayStart = diaryDayStartTime(settingsRepository.getDiaryDayStartHour()),
                loggedDayRule = loggedDayRule,
                history = history,
            ),
        )
    }
}

data class MealReminderPlan(
    val loggedDayRule: LoggedDayRule,
    val slots: List<MealSlotPlan>,
)
