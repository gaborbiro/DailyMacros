package dev.gaborbiro.dailymacros.features.trends.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.features.shared.mealreminders.LoggedDayRuleProvider
import dev.gaborbiro.dailymacros.features.trends.TrendsLoggedDayRuleProvider
import dev.gaborbiro.dailymacros.features.trends.TrendsPreferences
import dev.gaborbiro.dailymacros.features.trends.TrendsPreferencesImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TrendsPreferencesModule {

    @Binds
    @Singleton
    abstract fun trendsPreferences(impl: TrendsPreferencesImpl): TrendsPreferences

    @Binds
    abstract fun loggedDayRuleProvider(impl: TrendsLoggedDayRuleProvider): LoggedDayRuleProvider
}
