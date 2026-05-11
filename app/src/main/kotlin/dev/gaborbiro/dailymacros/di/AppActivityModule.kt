package dev.gaborbiro.dailymacros.di

import android.app.Activity
import android.app.Application
import androidx.activity.ComponentActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dev.gaborbiro.dailymacros.features.modal.ModalNavigator
import dev.gaborbiro.dailymacros.features.modal.ModalNavigatorImpl
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
    fun createPublicDocumentUseCase(activity: Activity): CreatePublicDocumentUseCase =
        CreatePublicDocumentUseCaseImpl(activity as ComponentActivity)

    @Provides
    @ActivityScoped
    fun openPublicDocumentUseCase(activity: Activity): OpenPublicDocumentUseCase =
        OpenPublicDocumentUseCaseImpl(activity as ComponentActivity)

    @Provides
    @ActivityScoped
    fun streamWriter(activity: Activity): StreamWriter =
        StreamWriter(activity)

    @Provides
    @ActivityScoped
    fun sharePublicUriLauncher(activity: Activity): SharePublicUriLauncher =
        SharePublicUriLauncher(activity)

    @Provides
    @ActivityScoped
    fun exportFoodDiaryUseCase(
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
    fun exportSqliteDatabaseUseCase(
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
    fun importSqliteDatabaseUseCase(
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
