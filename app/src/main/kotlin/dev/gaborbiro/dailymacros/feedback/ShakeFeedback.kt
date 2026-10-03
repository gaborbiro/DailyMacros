package dev.gaborbiro.dailymacros.feedback

import android.app.Application
import android.util.Log
import androidx.navigation.NavController
import com.shakebugs.shake.LogLevel
import com.shakebugs.shake.Shake
import com.shakebugs.shake.ShakeScreen
import dev.gaborbiro.dailymacros.BuildConfig

/**
 * Shake (shakebugs.com) in-app feedback for closed-alpha testers: a floating button (and a
 * device shake) opens Shake's own "New ticket" screen with a screenshot, the last ~15s of screen
 * recording, activity history and device/app info already attached.
 *
 * Everything Shake-specific is kept here. Removing Shake = delete this file, its two call sites
 * (App.onCreate, MainActivity), the dependency/SHAKE_API_KEY wiring in app/build.gradle.kts, and
 * the READ_EXTERNAL_STORAGE removal in AndroidManifest.xml.
 *
 * Meal photos/details are intentionally NOT masked (no Shake.addPrivateView / FLAG_SECURE):
 * when a tester says the macros are wrong, the photo and surrounding state are the evidence.
 */
object ShakeFeedback {

    private const val TAG = "ShakeFeedback"
    private const val METADATA_CLIENT_ID = "clientId"
    private const val METADATA_SCREEN = "screen"

    private var started = false

    fun start(application: Application, clientId: String) {
        val apiKey = BuildConfig.SHAKE_API_KEY
        if (apiKey.isBlank()) {
            Log.i(TAG, "No Shake API key in this build; tester feedback is disabled")
            return
        }
        // Defaults are spelled out on purpose so each capability can be switched off with a
        // one-line change after the alpha evaluation.
        Shake.getReportConfiguration().apply {
            // Invocation: floating button + device shake only. Screenshot invocation would make
            // Shake request storage access, and the edge pan is off as requested.
            isShowFloatingReportButton = true
            isInvokeShakeOnShakeDeviceEvent = true
            isInvokeShakeOnScreenshot = false
            isInvokeShakeOnRightEdgePan = false
            defaultScreen = ShakeScreen.NEW

            // Attachments
            isScreenshotIncluded = true
            // Shake: "Turn off this feature in production!" Kept on deliberately for the alpha
            // evaluation. Asks the tester for entire-screen capture consent (MediaProjection).
            isAutoVideoRecording = true

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
        started = true
    }

    /**
     * Shake only sees Activity changes, and almost every screen here is a Compose nav
     * destination inside MainActivity. Reports the current route as ticket metadata and logs
     * each navigation to the activity history. Only the route pattern is reported, never its
     * arguments.
     */
    suspend fun trackScreens(navController: NavController) {
        if (!started) return
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route?.substringBefore('?') ?: return@collect
            Shake.setMetadata(METADATA_SCREEN, route)
            Shake.log(LogLevel.INFO, "Navigated to $route")
        }
    }
}
