package dev.gaborbiro.dailymacros.repositories.backup.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.repositories.backup.BackupRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.backup.domain.BackupRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupRepositoryModule {

    @Binds
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
