package dev.gaborbiro.dailymacros.repositories.billing

import android.content.Context
import androidx.core.content.edit

/** Remembers the last purchase token successfully posted to `verifySubscription`, so a repeat app-start doesn't re-enqueue work for a token already confirmed. */
internal class BillingPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var lastVerifiedPurchaseToken: String?
        get() = prefs.getString(KEY_LAST_VERIFIED_TOKEN, null)
        set(value) {
            prefs.edit { putString(KEY_LAST_VERIFIED_TOKEN, value) }
        }

    private companion object {
        const val PREFS_NAME = "billing_prefs"
        const val KEY_LAST_VERIFIED_TOKEN = "last_verified_purchase_token"
    }
}
