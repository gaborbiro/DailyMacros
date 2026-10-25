package dev.gaborbiro.dailymacros.feedback.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsSink
import dev.gaborbiro.dailymacros.feedback.ShakeAnalyticsSink

@Module
@InstallIn(SingletonComponent::class)
internal object ShakeFeedbackModule {

    @Provides
    @IntoSet
    fun shakeAnalyticsSink(): AnalyticsSink = ShakeAnalyticsSink
}
