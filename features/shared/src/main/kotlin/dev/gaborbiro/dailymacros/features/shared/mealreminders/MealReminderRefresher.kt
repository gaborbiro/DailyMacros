package dev.gaborbiro.dailymacros.features.shared.mealreminders

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Re-plans meal reminders now. For screens that change the diary in a way reminders depend on
 * (e.g. moving a meal's time), so they don't rely on the diary observer alone.
 */
fun interface MealReminderRefresher {
    /** [trigger] shows up in the Settings > Debug readout's "Last pass" line. */
    fun refresh(trigger: String)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MealReminderModule {

    @Binds
    abstract fun refresher(impl: MealReminderCoordinator): MealReminderRefresher
}
