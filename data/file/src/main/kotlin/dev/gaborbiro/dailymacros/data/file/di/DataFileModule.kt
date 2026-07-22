package dev.gaborbiro.dailymacros.data.file.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.data.file.FileStoreFactoryImpl
import dev.gaborbiro.dailymacros.data.file.domain.FileStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataFileModule {

    @Provides
    @Singleton
    @FileStorePublicBucketPersistent
    fun persistentPublicFileStore(@ApplicationContext context: Context): FileStore =
        FileStoreFactoryImpl(context).getStore("thumbnails", keepFiles = true)

    @Provides
    @Singleton
    @FileStorePublicBucketEphemeral
    fun ephemeralPublicFileStore(@ApplicationContext context: Context): FileStore =
        FileStoreFactoryImpl(context).getStore("photos", keepFiles = false)
}
