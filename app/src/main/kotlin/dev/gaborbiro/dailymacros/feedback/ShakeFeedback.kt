package dev.gaborbiro.dailymacros.feedback

import android.app.Application
import com.shakebugs.shake.Shake
import com.shakebugs.shake.ShakeScreen
import dev.gaborbiro.dailymacros.BuildConfig

/**
 * Shake (shakebugs.com) in-app feedback for closed-alpha testers: a floating button opens Shake's
 * own "New ticket" screen with a screenshot, activity history and device/app info already
 * attached.
 *
 * Everything Shake-specific is kept in this package. Removing Shake = delete the package, its call
 * site in App.onCreate, the dependency (Shake + curtains)/SHAKE_API_KEY wiring in
 * app/build.gradle.kts, the button-size override res/layout/shake_sdk_view_report_button.xml, and
 * the READ_EXTERNAL_STORAGE removal in AndroidManifest.xml. Screen names, custom keys, events and
 * non-fatal errors reach Shake through AnalyticsLogger (see ShakeAnalyticsSink).
 *
 * Meal photos/details are intentionally NOT masked (no Shake.addPrivateView / FLAG_SECURE):
 * when a tester says the macros are wrong, the photo and surrounding state are the evidence.
 */
object ShakeFeedback {

    private const val METADATA_CLIENT_ID = "clientId"

    var isStarted = false
        private set

    fun start(application: Application, clientId: String) {
        // Never blank: the build refuses to package an app without it (verifyShakeApiKey).
        val apiKey = BuildConfig.SHAKE_API_KEY
        // Defaults are spelled out on purpose so each capability can be switched off with a
        // one-line change after the alpha evaluation.
        Shake.getReportConfiguration().apply {
            // Invocation: floating button only (kept above dialogs by ShakeButtonAboveDialogs).
            // Device shake was tried in the alpha evaluation and not needed. Screenshot invocation
            // would make Shake request storage access.
            isShowFloatingReportButton = true
            isInvokeShakeOnShakeDeviceEvent = false
            isInvokeShakeOnScreenshot = false
            isInvokeShakeOnRightEdgePan = false
            defaultScreen = ShakeScreen.NEW

            // Attachments
            isScreenshotIncluded = true
            // Off after the alpha evaluation: it asked for entire-screen capture consent on every
            // launch, and the screenshot plus activity history were enough.
            isAutoVideoRecording = false

            // Diagnostic context: activity history (lifecycle events, screen changes, our
            // Shake.log calls, console logs) and the 60s "black box" (memory/disk/network/
            // orientation/battery charts).
            isEnableActivityHistory = true
            isConsoleLogsEnabled = true
            isEnableBlackBox = true
            // Redacts emails, IPs, card numbers and bearer tokens from captured text.
            isSensitiveDataRedactionEnabled = true
        }
        // Crashlytics already owns crash reporting; this is about tester feedback only.
        Shake.setCrashReportingEnabled(false)

        Shake.start(application, apiKey)
        Shake.setMetadata(METADATA_CLIENT_ID, clientId)
        ShakeButtonAboveDialogs.install()
        isStarted = true
    }
}
