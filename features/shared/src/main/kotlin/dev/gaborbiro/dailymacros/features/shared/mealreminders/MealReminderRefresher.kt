package dev.gaborbiro.dailymacros.features.shared.mealreminders

/**
 * Re-plans meal reminders now. For screens that change the diary in a way reminders depend on
 * (e.g. moving a meal's time), so they don't rely on the diary observer alone.
 */
fun interface MealReminderRefresher {
    /** [trigger] shows up in the Settings > Debug readout's "Last pass" line. */
    fun refresh(trigger: String)
}
