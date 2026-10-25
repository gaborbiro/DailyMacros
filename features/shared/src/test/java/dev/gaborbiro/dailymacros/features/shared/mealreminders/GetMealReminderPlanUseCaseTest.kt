package dev.gaborbiro.dailymacros.features.shared.mealreminders

import dev.gaborbiro.dailymacros.repositories.settings.domain.model.MealReminderSlotHistory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.toKotlinInstant

class GetMealReminderPlanUseCaseTest {

    // Stored reminder times come back in the device zone, so plan in it too.
    private val now = ZonedDateTime.of(2024, 2, 7, 6, 0, 0, 0, ZoneId.systemDefault())

    private val repository = FakeMealReminderRepository()

    private val useCase = GetMealReminderPlanUseCase(
        getMealReminderInputsUseCase = GetMealReminderInputsUseCase(
            recordsRepository = FakeRecordsRepository(),
            settingsRepository = FakeSettingsRepository(),
            loggedDayRuleProvider = { LoggedDayRule.AnyRecord },
        ),
        mealReminderRepository = repository,
    )

    @Test
    fun `without history every slot is reminded at its default time`() = runTest {
        val plan = useCase.execute(now = now)

        assertEquals(LoggedDayRule.AnyRecord, plan.loggedDayRule)
        assertEquals(
            listOf(
                MealReminderDecision.Remind(now.withHour(8), ReminderStyle.PHOTO_PROMPT),
                MealReminderDecision.Remind(now.withHour(12).withMinute(30), ReminderStyle.PHOTO_PROMPT),
                MealReminderDecision.Remind(now.withHour(19), ReminderStyle.PHOTO_PROMPT),
            ),
            plan.slots.map { it.decision },
        )
    }

    @Test
    fun `plans with the stored reminder history by default`() = runTest {
        repository.slotHistory["lunch"] = MealReminderSlotHistory(lastRemindedAt = now.minusHours(1).toInstant().toKotlinInstant())

        val plan = useCase.execute(now = now)

        assertEquals(
            MealReminderDecision.Silent(SilenceReason.ALREADY_REMINDED),
            plan.slots.single { it.slot == MealSlot.LUNCH }.decision,
        )
    }
}
