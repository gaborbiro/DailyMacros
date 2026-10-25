package dev.gaborbiro.dailymacros.features.shared.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.features.shared.healthconnect.HealthConnectClientProvider
import dev.gaborbiro.dailymacros.features.shared.healthconnect.HealthConnectClientProviderImpl

@Module
@InstallIn(SingletonComponent::class)
internal abstract class HealthConnectModule {

    @Binds
    abstract fun clientProvider(impl: HealthConnectClientProviderImpl): HealthConnectClientProvider
}
