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
import javax.inject.Named
import javax.inject.Singleton

private val Context.appPrefsDataStore: DataStore<Preferences> by preferencesDataStore("appPrefs")

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AppPrefsModule {

    @Binds
    @Singleton
    abstract fun provideContext(application: Application): Context

    @Binds
    @Singleton
    abstract fun provideAppPrefs(impl: AppPrefsImpl): AppPrefs

    companion object {

        @Provides
        @Singleton
        @Named("appPrefs")
        fun provideDataStore(context: Context): DataStore<Preferences> {
            return context.appPrefsDataStore
        }
    }
}