package dev.gaborbiro.dailymacros.data.image.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.data.image.ImageStoreImpl
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore

@Module
@InstallIn(SingletonComponent::class)
abstract class DataImageModule {

    @Binds
    abstract fun bindImageStore(impl: ImageStoreImpl): ImageStore
}
