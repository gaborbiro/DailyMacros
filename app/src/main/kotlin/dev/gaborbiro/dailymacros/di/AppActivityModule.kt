package dev.gaborbiro.dailymacros.di

import android.app.Activity
import android.app.Application
import androidx.activity.ComponentActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dev.gaborbiro.dailymacros.features.settings.export.CreatePublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.settings.export.CreatePublicDocumentUseCaseImpl
import dev.gaborbiro.dailymacros.features.settings.export.OpenPublicDocumentUseCase
import dev.gaborbiro.dailymacros.features.settings.export.OpenPublicDocumentUseCaseImpl
import dev.gaborbiro.dailymacros.features.settings.export.SharePublicUriLauncher
import dev.gaborbiro.dailymacros.features.settings.export.StreamWriter
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ExportFoodDiaryUseCase
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ExportSqliteDatabaseUseCase
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ImportSqliteDatabaseUseCase
import dev.gaborbiro.dailymacros.repositories.backup.domain.BackupRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository

@Module
@InstallIn(ActivityComponent::class)
object AppActivityModule {

    @Provides
    @ActivityScoped
    fun provideCreatePublicDocumentUseCase(activity: Activity): CreatePublicDocumentUseCase =
        CreatePublicDocumentUseCaseImpl(activity as ComponentActivity)

    @Provides
    @ActivityScoped
    fun provideOpenPublicDocumentUseCase(activity: Activity): OpenPublicDocumentUseCase =
        OpenPublicDocumentUseCaseImpl(activity as ComponentActivity)

    @Provides
    @ActivityScoped
    fun provideExportFoodDiaryUseCase(
        recordsRepository: RecordsRepository,
        createPublicDocumentUseCase: CreatePublicDocumentUseCase,
        streamWriter: StreamWriter,
        sharePublicUriLauncher: SharePublicUriLauncher,
    ): ExportFoodDiaryUseCase =
        ExportFoodDiaryUseCase(
            recordRepository = recordsRepository,
            createPublicDocumentUseCase = createPublicDocumentUseCase,
            streamWriter = streamWriter,
            sharePublicUriLauncher = sharePublicUriLauncher,
        )

    @Provides
    @ActivityScoped
    fun provideExportSqliteDatabaseUseCase(
        backupRepository: BackupRepository,
        createPublicDocumentUseCase: CreatePublicDocumentUseCase,
        streamWriter: StreamWriter,
    ): ExportSqliteDatabaseUseCase =
        ExportSqliteDatabaseUseCase(
            backupRepository = backupRepository,
            createPublicDocumentUseCase = createPublicDocumentUseCase,
            streamWriter = streamWriter,
        )

    @Provides
    @ActivityScoped
    fun provideImportSqliteDatabaseUseCase(
        activity: Activity,
        application: Application,
        backupRepository: BackupRepository,
        openPublicDocumentUseCase: OpenPublicDocumentUseCase,
    ): ImportSqliteDatabaseUseCase =
        ImportSqliteDatabaseUseCase(
            application = application,
            backupRepository = backupRepository,
            openPublicDocumentUseCase = openPublicDocumentUseCase,
            activityProvider = { activity as ComponentActivity },
        )
}
