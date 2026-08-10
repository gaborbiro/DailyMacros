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

    // Read before anything else can call `userUUID` and lazily create that key, so this
    // stays true only for installs that already existed before onboarding shipped -
    // those should never be sent through it retroactively.
    private val isPreExistingInstall = prefs.getString(KEY_USER_UUID, null) != null

    @OptIn(ExperimentalUuidApi::class)
    val userUUID: String
        get() {
            val existing = prefs.getString(KEY_USER_UUID, null)
            if (existing != null) return existing

            val newUuid = ThreeWordId.random()
            prefs.edit { putString(KEY_USER_UUID, newUuid) }
            return newUuid
        }

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, isPreExistingInstall)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETE, value) }
}