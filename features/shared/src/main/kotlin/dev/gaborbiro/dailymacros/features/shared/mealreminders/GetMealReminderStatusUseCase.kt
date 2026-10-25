package dev.gaborbiro.dailymacros.features.shared.mealreminders

import dev.gaborbiro.dailymacros.repositories.settings.domain.MealReminderRepository
import java.time.ZonedDateTime
import javax.inject.Inject

/** Whether reminders are on and can be posted, and what the last pass did. */
class GetMealReminderStatusUseCase @Inject constructor(
    private val mealReminderRepository: MealReminderRepository,
    private val notifier: MealReminderNotifier,
) {

    fun execute() = MealReminderStatus(
        enabled = mealReminderRepository.isEnabled(),
        notificationsAllowed = notifier.canPost(),
        lastPass = mealReminderRepository.getLastPass()?.toStatusLastPass(),
    )
}

data class MealReminderStatus(
    val enabled: Boolean,
    val notificationsAllowed: Boolean,
    val lastPass: LastPass?,
) {
    /** [summary] starts with the pass's trigger, e.g. "settings: posted lunch". */
    data class LastPass(val at: ZonedDateTime, val summary: String, val nextWakeUp: ZonedDateTime?)
}
