package dev.gaborbiro.dailymacros.features.healthconnect

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/**
 * Health Connect refuses to show its permission-request dialog at all (silently returning an
 * empty grant set, no UI) unless the requesting app declares a "rationale" it can link to - see
 * the two manifest entries pointing here (the pre-Android-14 rationale intent and the Android
 * 14+ ViewPermissionUsageActivity alias). This just opens the privacy policy and closes; it's
 * not meant to be seen, only to satisfy that requirement so HealthConnectSyncUseCase's
 * permission request actually prompts the user.
 */
class HealthConnectRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
        finish()
    }

    companion object {
        // Kept in sync with features/settings' settings_privacy_policy_url string manually -
        // pulling that resource across modules isn't worth the coupling for a link this activity
        // never actually shows on screen.
        private const val PRIVACY_POLICY_URL = "https://github.com/gaborbiro/DailyMacros/blob/master/PRIVACY.md"
    }
}
