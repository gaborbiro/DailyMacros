package dev.gaborbiro.dailymacros.core.analytics

import android.os.Bundle

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
