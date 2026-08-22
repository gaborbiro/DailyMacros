package dev.gaborbiro.dailymacros.features.settings.export

import android.app.Activity
import android.content.Intent

internal object ProcessRestarter {

    // Read by MainActivity (a different module, hence a plain string literal
    // rather than a shared constant - conventional for Android intent-extra
    // keys) in the freshly-restarted process. This function is only ever
    // called right after a local/cloud restore (see SettingsViewModel's three
    // RestartApplication emissions), so it's always appropriate to mark this.
    // The point of routing it through the restart rather than writing
    // AppPrefs.hasCompletedOnboarding directly beforehand: SharedPreferences
    // caches a file's entire contents in memory for the life of the process,
    // and a write from the pre-restore process would silently re-serialize
    // that stale in-memory snapshot over whatever the restore just wrote to
    // app_prefs.xml on disk. A fresh process reads the real, just-restored
    // file on its first access, so there's nothing stale to clobber.
    const val EXTRA_JUST_RESTORED = "dev.gaborbiro.dailymacros.EXTRA_JUST_RESTORED"

    fun restartApplication(activity: Activity) {
        val intent =
            activity.packageManager.getLaunchIntentForPackage(activity.packageName)
                ?: return
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
        )
        intent.putExtra(EXTRA_JUST_RESTORED, true)
        activity.startActivity(intent)
        activity.finishAffinity()
        Runtime.getRuntime().exit(0)
    }
}
