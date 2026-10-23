package dev.gaborbiro.dailymacros.feedback

import android.os.Bundle
import com.shakebugs.shake.LogLevel
import com.shakebugs.shake.Shake
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsSink

/**
 * Mirrors AnalyticsLogger into Shake tickets. Crashlytics custom keys become ticket metadata
 * (a snapshot of the latest values when the ticket is sent); screen views also set the `screen`
 * metadata; screen views, events and non-fatal errors become entries in the ticket's activity
 * history timeline.
 */
internal object ShakeAnalyticsSink : AnalyticsSink {

    private const val METADATA_SCREEN = "screen"

    // Keeps one log entry readable; release stack traces are R8-obfuscated beyond the top frames anyway.
    private const val MAX_ERROR_LOG_LENGTH = 2000

    override fun onCustomData(key: String, value: String) {
        if (!ShakeFeedback.isStarted) return
        Shake.setMetadata(key, value)
    }

    override fun onEvent(name: String, params: Bundle?) {
        if (!ShakeFeedback.isStarted) return
        Shake.log(LogLevel.INFO, "Event: $name${params.describe()}")
    }

    override fun onScreenView(screenName: String, args: Bundle?) {
        if (!ShakeFeedback.isStarted) return
        Shake.setMetadata(METADATA_SCREEN, screenName)
        Shake.log(LogLevel.INFO, "Screen: $screenName${args.describe()}")
    }

    override fun onError(t: Throwable) {
        if (!ShakeFeedback.isStarted) return
        Shake.log(LogLevel.ERROR, t.stackTraceToString().take(MAX_ERROR_LOG_LENGTH))
    }

    @Suppress("DEPRECATION")
    private fun Bundle?.describe(): String =
        this?.keySet()
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = " {", postfix = "}") { key -> "$key=${get(key)}" }
            .orEmpty()
}

@Module
@InstallIn(SingletonComponent::class)
internal object ShakeFeedbackModule {

    @Provides
    @IntoSet
    fun shakeAnalyticsSink(): AnalyticsSink = ShakeAnalyticsSink
}
