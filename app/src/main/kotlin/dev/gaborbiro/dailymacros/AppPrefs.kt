package dev.gaborbiro.dailymacros

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.util.ThreeWordId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPrefs @Inject constructor(
    @ApplicationContext context: Context
) {

    companion object {
        // Historic key name, from when this was a UUID. Never rename the string: doing so
        // would silently give every existing install a new client ID.
        private const val KEY_CLIENT_ID = "user_uuid_3"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Separate, backup-excluded file: the client ID identifies *this install* for analytics
    // (see MainActivity's analyticsLogger.setUserId call), the AI proxy and Remote Config.
    // It must never travel via OS auto-backup, device transfer, or the in-app Drive/local
    // backup - restoring it onto a different install would make analytics think a fresh
    // install is a continuation of the old one. Kept separate from repositories/settings' local_only_prefs since that
    // file belongs to a different module/class - sharing a file across module boundaries
    // is how key collisions happen.
    private val localOnlyPrefs = context.getSharedPreferences("local_only_app_prefs", Context.MODE_PRIVATE)

    // Read before anything else can call `clientId` and lazily create that key, so this
    // stays true only for installs that already existed before onboarding shipped -
    // those should never be sent through it retroactively.
    private val isPreExistingInstall = localOnlyPrefs.getString(KEY_CLIENT_ID, null) != null

    /**
     * The three-word client ID (e.g. "apple-fox-moon"), generated on first access. Shown in
     * Settings as "Client ID" and quoted by users in support emails; the backend stores it
     * as `clientId` (X-Client-Id header, `clientIds/{clientId}`, `users/{uid}.clientId`).
     * Not to be confused with the Firebase Auth `uid`.
     */
    val clientId: String
        get() {
            val existing = localOnlyPrefs.getString(KEY_CLIENT_ID, null)
            if (existing != null) return existing

            val newClientId = ThreeWordId.random()
            localOnlyPrefs.edit { putString(KEY_CLIENT_ID, newClientId) }
            return newClientId
        }

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, isPreExistingInstall)
        // Written synchronously: callers that set this immediately trigger a process
        // restart (Runtime.getRuntime().exit), which would race an async apply() write.
        set(value) = prefs.edit(commit = true) { putBoolean(KEY_ONBOARDING_COMPLETE, value) }
}