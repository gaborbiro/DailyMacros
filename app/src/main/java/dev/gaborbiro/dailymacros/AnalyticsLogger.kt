package dev.gaborbiro.dailymacros

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics

class AnalyticsLogger {

    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }
    private val crashlytics: FirebaseCrashlytics by lazy { Firebase.crashlytics }

    fun setUserId(userId: String?) {
        analytics.setUserId(userId)
        crashlytics.setUserId(userId ?: "")
    }

    fun setCustomDataForNextCrash(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    fun logEvent(name: String, params: Bundle? = null) {
        val finalName = name.replace(Regex("[^a-zA-Z0-9_]"), "_").take(40)
        analytics.logEvent(finalName, params)
    }

    fun logScreenView(screenName: String, args: Bundle? = null) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            args?.keySet()?.forEach { key ->
                param(key, args.get(key).toString())
            }
        }
    }

    fun logError(t: Throwable) {
        t.printStackTrace()
        crashlytics.recordException(t)
    }
}
