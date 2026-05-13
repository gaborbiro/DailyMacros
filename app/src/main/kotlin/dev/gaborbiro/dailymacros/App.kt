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
import dev.gaborbiro.dailymacros.features.widget.WidgetAutoReloader
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
        EntryPointAccessors.fromApplication(this, AppBootstrapEntryPoint::class.java)
            .widgetAutoReloader()
            .start()
    }
}
