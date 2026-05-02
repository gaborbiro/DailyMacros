package dev.gaborbiro.dailymacros.features.settings.export

import android.app.Activity
import android.content.Intent

internal object ProcessRestarter {

    fun restartApplication(activity: Activity) {
        val intent =
            activity.packageManager.getLaunchIntentForPackage(activity.packageName)
                ?: return
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
        )
        activity.startActivity(intent)
        activity.finishAffinity()
        Runtime.getRuntime().exit(0)
    }
}
