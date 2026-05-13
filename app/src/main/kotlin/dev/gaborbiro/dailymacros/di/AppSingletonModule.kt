package dev.gaborbiro.dailymacros.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.AppPrefs
import dev.gaborbiro.dailymacros.BuildConfig
import dev.gaborbiro.dailymacros.features.main.MineMealVariabilityWorker
import dev.gaborbiro.dailymacros.features.settings.EnqueueMealVariabilityMining
import dev.gaborbiro.dailymacros.features.settings.SettingsAppInfo
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppSingletonModule {

    @Provides
    @Singleton
    fun gson(): Gson = Gson()

    @Provides
    @Singleton
    fun settingsAppInfo(appPrefs: AppPrefs): SettingsAppInfo =
        object : SettingsAppInfo {
            override val versionLabel: String
                get() = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})  |  UserID: ${appPrefs.userUUID}"
        }

    @Provides
    @Singleton
    fun enqueueMealVariabilityMining(@ApplicationContext context: Context): EnqueueMealVariabilityMining =
        EnqueueMealVariabilityMining { MineMealVariabilityWorker.enqueue(context) }
}
