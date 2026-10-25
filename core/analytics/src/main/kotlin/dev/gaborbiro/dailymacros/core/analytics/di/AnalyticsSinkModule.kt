package dev.gaborbiro.dailymacros.core.analytics.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsSink

@Module
@InstallIn(SingletonComponent::class)
internal interface AnalyticsSinkModule {

    /** Declares the (possibly empty) set, so builds with no sink bound still compile. */
    @Multibinds
    fun analyticsSinks(): Set<AnalyticsSink>
}
