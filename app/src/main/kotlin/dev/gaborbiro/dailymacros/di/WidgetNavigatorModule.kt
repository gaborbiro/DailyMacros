package dev.gaborbiro.dailymacros.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.WidgetNavigatorImpl
import dev.gaborbiro.dailymacros.features.widget.WidgetNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetNavigatorModule {

    @Binds
    @Singleton
    abstract fun bindWidgetNavigator(impl: WidgetNavigatorImpl): WidgetNavigator
}
