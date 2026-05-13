package dev.gaborbiro.dailymacros.repositories.settings.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.repositories.settings.SettingsRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsRepositoryModule {

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
