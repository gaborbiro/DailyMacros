package dev.gaborbiro.dailymacros.features.shared.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.features.shared.mealreminders.MealReminderCoordinator
import dev.gaborbiro.dailymacros.features.shared.mealreminders.MealReminderRefresher

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MealReminderModule {

    @Binds
    abstract fun refresher(impl: MealReminderCoordinator): MealReminderRefresher
}
