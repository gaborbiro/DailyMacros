package dev.gaborbiro.nutrition.app_prefs.di

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.nutrition.app_prefs.AppPrefsImpl
import dev.gaborbiro.nutrition.app_prefs.domain.AppPrefs
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("appPrefs")

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AppPrefsModule {

    @Binds
    @Singleton
    abstract fun provideContext(application: Application): Context

    companion object {

        @Provides
        @Singleton
        fun provideAppPrefs(context: Context): AppPrefs {
            return AppPrefsImpl(context.dataStore)
        }
    }
}