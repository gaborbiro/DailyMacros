package dev.gaborbiro.dailymacros.data.db.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.data.db.AppDatabase
import dev.gaborbiro.dailymacros.data.db.RecordsDAO
import dev.gaborbiro.dailymacros.data.db.RequestStatusDAO
import dev.gaborbiro.dailymacros.data.db.TemplatesDAO
import dev.gaborbiro.dailymacros.data.db.VariabilityDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataDbModule {

    @Provides
    @Singleton
    fun appDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.build(context)

    @Provides
    fun recordsDao(db: AppDatabase): RecordsDAO = db.recordsDAO()

    @Provides
    fun templatesDao(db: AppDatabase): TemplatesDAO = db.templatesDAO()

    @Provides
    fun requestStatusDao(db: AppDatabase): RequestStatusDAO = db.requestStatusDAO()

    @Provides
    fun variabilityDao(db: AppDatabase): VariabilityDao = db.variabilityDao()
}
