package dev.gaborbiro.dailymacros.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.AppPrefs
import dev.gaborbiro.dailymacros.BuildConfig
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.db.AppDatabase
import dev.gaborbiro.dailymacros.data.file.FileStoreFactoryImpl
import dev.gaborbiro.dailymacros.data.file.domain.FileStore
import dev.gaborbiro.dailymacros.data.image.ImageStoreImpl
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.settings.EnqueueMealVariabilityMining
import dev.gaborbiro.dailymacros.features.settings.SettingsAppInfo
import dev.gaborbiro.dailymacros.features.settings.SettingsPrefs
import dev.gaborbiro.dailymacros.features.main.MineMealVariabilityWorker
import dev.gaborbiro.dailymacros.features.modal.ModalNavigator
import dev.gaborbiro.dailymacros.features.modal.ModalNavigatorImpl
import dev.gaborbiro.dailymacros.features.modal.ModalUiMapper
import dev.gaborbiro.dailymacros.features.modal.usecase.BuildRecordDetailsViewDialogUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.GetVariabilityMatchForTemplateUseCase
import dev.gaborbiro.dailymacros.features.modal.usecase.OpenTemplateVariantPickerFromRecordDetailsUseCase
import dev.gaborbiro.dailymacros.features.overview.OverviewPrefs
import dev.gaborbiro.dailymacros.features.settings.variability.MineMealVariabilityPreviewUseCase
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.backup.BackupRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.backup.domain.BackupRepository
import dev.gaborbiro.dailymacros.repositories.records.RecordsApiMapper
import dev.gaborbiro.dailymacros.repositories.records.RecordsRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.records.RequestStatusRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.records.TemplateVariabilityPreviewMapper
import dev.gaborbiro.dailymacros.repositories.records.VariabilityProfileMapper
import dev.gaborbiro.dailymacros.repositories.records.VariabilityRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.RequestStatusRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.VariabilityRepository
import dev.gaborbiro.dailymacros.repositories.settings.SettingsMapper
import dev.gaborbiro.dailymacros.repositories.settings.SettingsRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppSingletonModule {

    @Provides
    @Singleton
    fun gson(): Gson = Gson()

    @Provides
    @Singleton
    fun analyticsLogger(): AnalyticsLogger = AnalyticsLogger()

    @Provides
    @Singleton
    fun appDatabase(): AppDatabase = AppDatabase.getInstance()

    @Provides
    @Singleton
    @FileStorePublicBucketPersistent
    fun persistentPublicFileStore(@ApplicationContext context: Context): FileStore =
        FileStoreFactoryImpl(context).getStore("public", keepFiles = true)

    @Provides
    @Singleton
    @FileStorePublicBucketEphemeral
    fun ephemeralPublicFileStore(@ApplicationContext context: Context): FileStore =
        FileStoreFactoryImpl(context).getStore("public", keepFiles = false)

    @Provides
    @Singleton
    fun imageStore(@FileStorePublicBucketPersistent fileStore: FileStore): ImageStore =
        ImageStoreImpl(fileStore)

    @Provides
    @Singleton
    fun settingsRepository(
        @ApplicationContext context: Context,
        mapper: SettingsMapper,
    ): SettingsRepository = SettingsRepositoryImpl(context, mapper)

    @Provides
    @Singleton
    fun recordsRepository(
        db: AppDatabase,
        mapper: RecordsApiMapper,
        imageStore: ImageStore,
        analyticsLogger: AnalyticsLogger,
    ): RecordsRepository =
        RecordsRepositoryImpl(
            templatesDAO = db.templatesDAO(),
            recordsDAO = db.recordsDAO(),
            mapper = mapper,
            imageStore = imageStore,
            analyticsLogger = analyticsLogger,
        )

    @Provides
    @Singleton
    fun requestStatusRepository(db: AppDatabase): RequestStatusRepository =
        RequestStatusRepositoryImpl(db.requestStatusDAO())

    @Provides
    @Singleton
    fun backupRepository(@ApplicationContext context: Context): BackupRepository =
        BackupRepositoryImpl(context)

    @Provides
    @Singleton
    fun variabilityRepository(
        db: AppDatabase,
        gson: Gson,
    ): VariabilityRepository =
        VariabilityRepositoryImpl(
            variabilityDao = db.variabilityDao(),
            profileMapper = VariabilityProfileMapper(gson),
        )

    @Provides
    @Singleton
    fun appPrefs(@ApplicationContext context: Context): AppPrefs = AppPrefs(context)

    @Provides
    @Singleton
    fun settingsPrefs(@ApplicationContext context: Context): SettingsPrefs =
        SettingsPrefs(context)

    @Provides
    @Singleton
    fun overviewPrefs(@ApplicationContext context: Context): OverviewPrefs =
        OverviewPrefs(context)

    @Provides
    @Singleton
    fun settingsAppInfo(appPrefs: AppPrefs): SettingsAppInfo =
        object : SettingsAppInfo {
            override val versionLabel: String
                get() = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})  |  UserID: ${appPrefs.userUUID}"
        }

    @Provides
    @Singleton
    fun enqueueMealVariabilityMining(@ApplicationContext context: Context): EnqueueMealVariabilityMining =
        EnqueueMealVariabilityMining { MineMealVariabilityWorker.enqueue(context) }

    @Provides
    @Singleton
    fun getVariabilityMatchForTemplateUseCase(
        variabilityRepository: VariabilityRepository,
        gson: Gson,
        previewMapper: TemplateVariabilityPreviewMapper,
    ): GetVariabilityMatchForTemplateUseCase =
        GetVariabilityMatchForTemplateUseCase(
            variabilityRepository = variabilityRepository,
            profileMapper = VariabilityProfileMapper(gson),
            previewMapper = previewMapper,
        )

    @Provides
    @Singleton
    fun openTemplateVariantPickerFromRecordDetailsUseCase(
        previewMapper: TemplateVariabilityPreviewMapper,
    ): OpenTemplateVariantPickerFromRecordDetailsUseCase =
        OpenTemplateVariantPickerFromRecordDetailsUseCase(
            templateVariabilityPreviewMapper = previewMapper,
        )

    @Provides
    @Singleton
    fun buildRecordDetailsViewDialogUseCase(
        getVariabilityMatchForTemplateUseCase: GetVariabilityMatchForTemplateUseCase,
        modalUiMapper: ModalUiMapper,
    ): BuildRecordDetailsViewDialogUseCase =
        BuildRecordDetailsViewDialogUseCase(
            getVariabilityMatchForTemplateUseCase = getVariabilityMatchForTemplateUseCase,
            uiMapper = modalUiMapper,
        )

    @Provides
    @Singleton
    fun mineMealVariabilityPreviewUseCase(
        recordsRepository: RecordsRepository,
        @ForJsonBodyChatGpt chatGPTRepository: ChatGPTRepository,
        variabilityRepository: VariabilityRepository,
    ): MineMealVariabilityPreviewUseCase =
        MineMealVariabilityPreviewUseCase(
            recordsRepository = recordsRepository,
            chatGPTRepository = chatGPTRepository,
            variabilityRepository = variabilityRepository,
        )

    @Provides
    @Singleton
    fun provideModalNavigator(): ModalNavigator =
        ModalNavigatorImpl()
}
