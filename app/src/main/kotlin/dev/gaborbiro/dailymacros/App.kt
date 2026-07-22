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
        bootstrap.widgetAutoReloader().start()
        // The photo monitor chain can die if a run is killed before it re-enqueues itself
        // (process death, force-stop). Re-arm on every process start; KEEP makes this a no-op
        // when the monitor is already scheduled.
        if (bootstrap.settingsRepository().getAutoPhotoRecognitionEnabled()) {
            PhotoMonitorWorker.enqueue(this)
        }
    }
}
