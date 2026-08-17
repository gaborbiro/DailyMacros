package dev.gaborbiro.dailymacros

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.features.shared.photodiary.PhotoMonitorWorker
import dev.gaborbiro.dailymacros.features.widgets.WidgetAutoReloader
import dev.gaborbiro.dailymacros.repositories.billing.domain.SubscriptionRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.PendingDriveSyncInfoStore
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.util.createNotificationChannels

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppWorkerFactoryEntryPoint {
    fun hiltWorkerFactory(): HiltWorkerFactory
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppBootstrapEntryPoint {
    fun widgetAutoReloader(): WidgetAutoReloader
    fun settingsRepository(): SettingsRepository
    fun subscriptionRepository(): SubscriptionRepository
    fun pendingDriveSyncInfoStore(): PendingDriveSyncInfoStore
}

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    companion object {
        lateinit var appContext: Context
    }

    override val workManagerConfiguration: Configuration
        get() {
            val workerFactory = EntryPointAccessors.fromApplication(
                this,
                AppWorkerFactoryEntryPoint::class.java,
            ).hiltWorkerFactory()

            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(this, workManagerConfiguration)
        }
        appContext = this
        createNotificationChannels()
        val bootstrap = EntryPointAccessors.fromApplication(this, AppBootstrapEntryPoint::class.java)
        // Applies bookkeeping staged by RestoreFromDriveUseCase, if a cloud restore restarted
        // the app since the last launch. Must run before anything else touches
        // SettingsRepository, since it's the first read/write of the (now freshly restored)
        // prefs file in this process.
        bootstrap.pendingDriveSyncInfoStore().consumeIfPresent()?.let { epochMs ->
            bootstrap.settingsRepository().setLastSyncedEpochMs(epochMs)
            bootstrap.settingsRepository().setAutoSyncErrorStatus(null)
        }
        bootstrap.widgetAutoReloader().start()
        // The photo monitor chain can die if a run is killed before it re-enqueues itself
        // (process death, force-stop). Re-arm on every process start; KEEP makes this a no-op
        // when the monitor is already scheduled.
        if (bootstrap.settingsRepository().getAutoPhotoRecognitionEnabled()) {
            PhotoMonitorWorker.enqueue(this)
        }
        // Stand-in for "next check-in" given RTDN is deferred: a subscription
        // cancelled/expired since last launch is only caught here or on the next
        // purchase event, not immediately.
        bootstrap.subscriptionRepository().refresh()
    }
}
