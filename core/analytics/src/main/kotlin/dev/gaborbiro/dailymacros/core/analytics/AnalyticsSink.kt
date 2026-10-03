package dev.gaborbiro.dailymacros.core.analytics

import android.os.Bundle
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * Optional extra destination for everything [AnalyticsLogger] records, on top of Firebase.
 * Contribute one with `@Provides @IntoSet` from any module (the app's tester-feedback SDK does);
 * with none bound, [AnalyticsLogger] behaves exactly as before.
 */
interface AnalyticsSink {
    fun onCustomData(key: String, value: String) {}
    fun onEvent(name: String, params: Bundle?) {}
    fun onScreenView(screenName: String, args: Bundle?) {}
    fun onError(t: Throwable) {}
}

@Module
@InstallIn(SingletonComponent::class)
internal interface AnalyticsSinkModule {

    /** Declares the (possibly empty) set, so builds with no sink bound still compile. */
    @Multibinds
    fun analyticsSinks(): Set<AnalyticsSink>
}
