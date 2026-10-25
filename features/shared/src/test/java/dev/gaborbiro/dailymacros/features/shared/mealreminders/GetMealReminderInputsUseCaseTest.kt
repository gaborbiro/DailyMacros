package dev.gaborbiro.dailymacros.features.shared.mealreminders

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.toKotlinInstant

class GetMealReminderInputsUseCaseTest {

    private val now = ZonedDateTime.of(2024, 2, 7, 6, 0, 0, 0, ZoneId.of("Europe/London"))

    @Test
    fun `loads the lookback plus two spare days, the diary day start and the logged-day rule`() = runTest {
        val records = FakeRecordsRepository()
        val rule = LoggedDayRule.MinimumCalories(1200)
        val useCase = GetMealReminderInputsUseCase(
            recordsRepository = records,
            settingsRepository = FakeSettingsRepository(diaryDayStartHour = 3),
            loggedDayRuleProvider = { rule },
        )

        val inputs = useCase.execute(now)

        assertEquals(now.minusDays(MealReminderPlanner.LOOKBACK_DAYS + 2).toInstant().toKotlinInstant(), records.requestedSince)
        assertEquals(LocalTime.of(3, 0), inputs.dayStart)
        assertEquals(rule, inputs.loggedDayRule)
        assertEquals(emptyList<Any>(), inputs.records)
    }
}
