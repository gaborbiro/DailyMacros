package dev.gaborbiro.dailymacros.features.trends

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TrendsPreferencesModule {

    @Binds
    @Singleton
    abstract fun trendsPreferences(impl: TrendsPreferencesImpl): TrendsPreferences
}
