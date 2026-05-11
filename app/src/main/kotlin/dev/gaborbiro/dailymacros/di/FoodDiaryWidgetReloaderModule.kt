package dev.gaborbiro.dailymacros.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.di.FoodDiaryWidgetReloaderImpl
import dev.gaborbiro.dailymacros.features.widget.FoodDiaryWidgetReloader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FoodDiaryWidgetReloaderModule {

    @Binds
    @Singleton
    abstract fun bindFoodDiaryWidgetReloader(impl: FoodDiaryWidgetReloaderImpl): FoodDiaryWidgetReloader
}
