package dev.gaborbiro.dailymacros

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.util.ThreeWordId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.ExperimentalUuidApi

@Singleton
class AppPrefs @Inject constructor(
    @ApplicationContext context: Context
) {

    companion object {
        private const val KEY_USER_UUID = "user_uuid_3"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Separate, backup-excluded file: this UUID identifies *this install* for analytics
    // (see MainActivity's analyticsLogger.setUserId call). It must never travel via OS
    // auto-backup, device transfer, or the in-app Drive/local backup - restoring it onto
    // a different install would make analytics think a fresh install is a continuation of
    // the old one. Kept separate from repositories/settings' local_only_prefs since that
    // file belongs to a different module/class - sharing a file across module boundaries
    // is how key collisions happen.
    private val localOnlyPrefs = context.getSharedPreferences("local_only_app_prefs", Context.MODE_PRIVATE)

    // Read before anything else can call `userUUID` and lazily create that key, so this
    // stays true only for installs that already existed before onboarding shipped -
    // those should never be sent through it retroactively.
    private val isPreExistingInstall = localOnlyPrefs.getString(KEY_USER_UUID, null) != null

    @OptIn(ExperimentalUuidApi::class)
    val userUUID: String
        get() {
            val existing = localOnlyPrefs.getString(KEY_USER_UUID, null)
            if (existing != null) return existing

            val newUuid = ThreeWordId.random()
            localOnlyPrefs.edit { putString(KEY_USER_UUID, newUuid) }
            return newUuid
        }

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, isPreExistingInstall)
        // Written synchronously: callers that set this immediately trigger a process
        // restart (Runtime.getRuntime().exit), which would race an async apply() write.
        set(value) = prefs.edit(commit = true) { putBoolean(KEY_ONBOARDING_COMPLETE, value) }
}