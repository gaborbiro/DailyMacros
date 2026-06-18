package dev.gaborbiro.dailymacros.repositories.backup.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.repositories.backup.CloudSyncRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.backup.domain.CloudSyncRepository
import okhttp3.OkHttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ForDrive

@Module
@InstallIn(SingletonComponent::class)
object CloudSyncProviderModule {

    @Provides
    @Singleton
    @ForDrive
    fun provideDriveOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CloudSyncBindingModule {

    @Binds
    abstract fun bindCloudSyncRepository(impl: CloudSyncRepositoryImpl): CloudSyncRepository
}
