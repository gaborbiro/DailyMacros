package dev.gaborbiro.dailymacros.repositories.records.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.repositories.records.RecordsRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.records.RequestStatusRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.records.VariabilityRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.RequestStatusRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.VariabilityRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RecordsRepositoryModule {

    @Binds
    abstract fun bindRecordsRepository(impl: RecordsRepositoryImpl): RecordsRepository

    @Binds
    abstract fun bindRequestStatusRepository(impl: RequestStatusRepositoryImpl): RequestStatusRepository

    @Binds
    abstract fun bindVariabilityRepository(impl: VariabilityRepositoryImpl): VariabilityRepository
}
