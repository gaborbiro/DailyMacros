package dev.gaborbiro.dailymacros.features.shared.mealreminders

import dev.gaborbiro.dailymacros.features.common.utils.diaryDayStartTime
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.time.toKotlinInstant

/** Loads what [MealReminderPlanner] plans from: the recent diary and the settings that shape it. */
class GetMealReminderInputsUseCase @Inject constructor(
    private val recordsRepository: RecordsRepository,
    private val settingsRepository: SettingsRepository,
    private val loggedDayRuleProvider: LoggedDayRuleProvider,
) {

    suspend fun execute(now: ZonedDateTime): MealReminderInputs = MealReminderInputs(
        // Two spare days so the lookback still covers whole diary days around the day-start shift.
        records = recordsRepository.getRecords(
            since = now.minusDays(MealReminderPlanner.LOOKBACK_DAYS + 2).toInstant().toKotlinInstant(),
        ),
        dayStart = diaryDayStartTime(settingsRepository.getDiaryDayStartHour()),
        loggedDayRule = loggedDayRuleProvider.get(),
    )
}

data class MealReminderInputs(
    val records: List<Record>,
    val dayStart: LocalTime,
    val loggedDayRule: LoggedDayRule,
)
