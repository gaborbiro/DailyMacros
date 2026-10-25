package dev.gaborbiro.dailymacros.features.shared.mealreminders

import dev.gaborbiro.dailymacros.repositories.settings.domain.MealReminderRepository
import java.time.ZonedDateTime
import javax.inject.Inject

/** Loads the history [MealReminderPlanner] needs and returns today's plan for each meal slot. */
class GetMealReminderPlanUseCase @Inject constructor(
    private val getMealReminderInputsUseCase: GetMealReminderInputsUseCase,
    private val mealReminderRepository: MealReminderRepository,
) {

    suspend fun execute(
        now: ZonedDateTime = ZonedDateTime.now(),
        history: Map<MealSlot, SlotReminderHistory> = mealReminderRepository.history(),
    ): MealReminderPlan {
        val inputs = getMealReminderInputsUseCase.execute(now)
        return MealReminderPlan(
            loggedDayRule = inputs.loggedDayRule,
            slots = MealReminderPlanner.plan(
                records = inputs.records,
                now = now,
                dayStart = inputs.dayStart,
                loggedDayRule = inputs.loggedDayRule,
                history = history,
            ),
        )
    }
}

data class MealReminderPlan(
    val loggedDayRule: LoggedDayRule,
    val slots: List<MealSlotPlan>,
)
