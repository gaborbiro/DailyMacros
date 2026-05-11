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
import dev.gaborbiro.dailymacros.data.db.AppDatabase
import dev.gaborbiro.dailymacros.util.createNotificationChannels

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppWorkerFactoryEntryPoint {
    fun hiltWorkerFactory(): HiltWorkerFactory
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
        WorkManager.initialize(this, workManagerConfiguration)
        appContext = this
        AppDatabase.init(this)
        createNotificationChannels()
    }
}
