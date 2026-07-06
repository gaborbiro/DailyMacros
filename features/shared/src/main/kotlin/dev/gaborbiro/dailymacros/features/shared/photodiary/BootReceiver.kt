package dev.gaborbiro.dailymacros.features.shared.photodiary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!settingsRepository.getAutoPhotoRecognitionEnabled()) return
        context.startForegroundService(Intent(context, PhotoMonitorService::class.java))
    }
}
