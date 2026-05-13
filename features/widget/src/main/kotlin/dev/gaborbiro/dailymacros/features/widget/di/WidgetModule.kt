package dev.gaborbiro.dailymacros.features.widget.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.features.shared.widget.FoodDiaryWidgetReloader
import dev.gaborbiro.dailymacros.features.widget.FoodDiaryWidgetReloaderImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {

    @Binds
    abstract fun bindFoodDiaryWidgetReloader(impl: FoodDiaryWidgetReloaderImpl): FoodDiaryWidgetReloader
}
