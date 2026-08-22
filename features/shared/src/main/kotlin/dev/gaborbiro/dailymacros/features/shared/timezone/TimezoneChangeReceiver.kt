package dev.gaborbiro.dailymacros.features.shared.timezone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import java.util.TimeZone
import javax.inject.Inject

/**
 * Manifest-registered so it fires even with the app process dead -- the same mechanism this
 * app already uses for BOOT_COMPLETED/MY_PACKAGE_REPLACED (see `WidgetBootReceiver`).
 * ACTION_TIMEZONE_CHANGED is exempt from the Android 8+ implicit-broadcast background
 * restrictions, so no extra runtime registration is needed.
 */
@AndroidEntryPoint
class TimezoneChangeReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIMEZONE_CHANGED) return
        settingsRepository.recordTimezoneEvent(TimeZone.getDefault().id, System.currentTimeMillis())
    }
}
