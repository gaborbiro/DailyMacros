package dev.gaborbiro.dailymacros.repositories.settings

import android.content.Context
import androidx.core.content.edit

/**
 * Holds prefs that must never leave this device via any backup path (OS auto-backup,
 * device transfer, or the in-app Drive/local backup in `BackupRepositoryImpl`) because
 * they're PII rather than app settings. Kept in its own file, separate from [SettingsRepositoryImpl]'s
 * general `prefs`, so both backup paths can exclude it by name alone instead of picking
 * fields out of a shared file.
 */
internal class LocalOnlyPrefsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var cloudSyncEmail: String?
        get() = prefs.getString(KEY_CLOUD_SYNC_EMAIL, null)
        set(value) {
            prefs.edit { if (value != null) putString(KEY_CLOUD_SYNC_EMAIL, value) else remove(KEY_CLOUD_SYNC_EMAIL) }
        }

    companion object {
        const val PREFS_NAME = "local_only_prefs"
        private const val KEY_CLOUD_SYNC_EMAIL = "cloud_sync_email"
    }
}
