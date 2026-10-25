package dev.gaborbiro.dailymacros.features.shared.mealreminders

import dev.gaborbiro.dailymacros.repositories.settings.domain.MealReminderRepository
import javax.inject.Inject

/** Saves the on/off switch. The next reminder pass applies it; see [MealReminderRefresher]. */
class SetMealRemindersEnabledUseCase @Inject constructor(
    private val mealReminderRepository: MealReminderRepository,
) {

    fun execute(enabled: Boolean) {
        mealReminderRepository.setEnabled(enabled)
    }
}
