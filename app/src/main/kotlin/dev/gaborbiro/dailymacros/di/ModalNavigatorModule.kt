package dev.gaborbiro.dailymacros.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.features.shared.ModalNavigator
import dev.gaborbiro.dailymacros.features.modal.ModalNavigatorImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ModalNavigatorModule {

    @Binds
    @Singleton
    abstract fun bindModalNavigator(impl: ModalNavigatorImpl): ModalNavigator
}
