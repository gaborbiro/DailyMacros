package dev.gaborbiro.dailymacros.features.common.workers

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.db.AppDatabase
import dev.gaborbiro.dailymacros.data.file.FileStoreFactoryImpl
import dev.gaborbiro.dailymacros.data.image.ImageStoreImpl
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.common.ChatGptOkHttpTimeouts
import dev.gaborbiro.dailymacros.features.settings.SettingsPrefs
import dev.gaborbiro.dailymacros.features.settings.variability.MEAL_VARIABILITY_MINING_OUTPUT_ERROR
import dev.gaborbiro.dailymacros.features.settings.variability.MEAL_VARIABILITY_MINING_UNIQUE_WORK
import dev.gaborbiro.dailymacros.features.settings.variability.MineMealVariabilityPreviewUseCase
import dev.gaborbiro.dailymacros.repositories.chatgpt.AuthInterceptor
import dev.gaborbiro.dailymacros.repositories.chatgpt.ChatGPTRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.ChatGPTService
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ContentEntryOutputContentDeserializer
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.OutputContentDeserializer
import dev.gaborbiro.dailymacros.repositories.records.RecordsApiMapper
import dev.gaborbiro.dailymacros.repositories.records.RecordsRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.records.VariabilityProfileMapper
import dev.gaborbiro.dailymacros.repositories.records.VariabilityRepositoryImpl
import dev.gaborbiro.dailymacros.util.CHANNEL_ID_FOREGROUND
import dev.gaborbiro.dailymacros.util.showTitleTextNotification
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.time.delay
import okhttp3.OkHttpClient
import okhttp3.java.net.cookiejar.JavaNetCookieJar
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class MineMealVariabilityWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {

    private val analyticsLogger: AnalyticsLogger by lazy { AnalyticsLogger() }

    private val imageStore: ImageStore by lazy {
        val fileStore = FileStoreFactoryImpl(appContext).getStore("public", keepFiles = true)
        ImageStoreImpl(fileStore)
    }

    private val database by lazy { AppDatabase.getInstance() }

    private val recordsRepository by lazy {
        RecordsRepositoryImpl(
            templatesDAO = database.templatesDAO(),
            recordsDAO = database.recordsDAO(),
            mapper = RecordsApiMapper(),
            imageStore = imageStore,
            analyticsLogger = analyticsLogger,
        )
    }

    private val mineMealVariabilityPreviewUseCase: MineMealVariabilityPreviewUseCase by lazy {
        val logger = HttpLoggingInterceptor().also {
            it.level = HttpLoggingInterceptor.Level.BODY
        }
        val authInterceptor = AuthInterceptor()
        val builder = OkHttpClient.Builder()
            .addNetworkInterceptor(logger)
            .addInterceptor(authInterceptor)
            .addNetworkInterceptor(authInterceptor)
            .also { ChatGptOkHttpTimeouts.applyJsonBodyTimeouts(it) }

        val okHttpClient = builder
            .cookieJar(JavaNetCookieJar(CookieManager()))
            .build()

        val gson = GsonBuilder()
            .registerTypeAdapter(OutputContent::class.java, OutputContentDeserializer())
            .registerTypeAdapter(
                object : TypeToken<ContentEntry<OutputContent>>() {}.type,
                ContentEntryOutputContentDeserializer(),
            )
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val chatGPTRepository = ChatGPTRepositoryImpl(
            service = retrofit.create(ChatGPTService::class.java),
        )
        val variabilityRepository = VariabilityRepositoryImpl(
            variabilityDao = database.variabilityDao(),
            profileMapper = VariabilityProfileMapper(Gson()),
        )
        MineMealVariabilityPreviewUseCase(
            recordsRepository = recordsRepository,
            chatGPTRepository = chatGPTRepository,
            variabilityRepository = variabilityRepository,
        )
    }

    override suspend fun doWork(): Result {
        return try {
            delay(0.5.seconds.toJavaDuration())
            setForegroundAsync(createForegroundInfo()).await()
            val preview = mineMealVariabilityPreviewUseCase.execute()
            val generatedAt = System.currentTimeMillis()
            val settingsPrefs = SettingsPrefs(applicationContext)
            settingsPrefs.variabilityMiningRequestJson = preview.requestJsonPretty
            settingsPrefs.variabilityMiningResponseJson = preview.responseJsonPretty
            settingsPrefs.variabilityMiningGeneratedAtEpochMs = generatedAt
            settingsPrefs.variabilityMiningRequestJsonExpansionBits = ""
            settingsPrefs.variabilityMiningResponseJsonExpansionBits = ""
            settingsPrefs.variabilityMiningRequestJsonSectionExpanded = false
            settingsPrefs.variabilityMiningResponseJsonSectionExpanded = false
            applicationContext.showTitleTextNotification(
                id = NOTIFICATION_ID_RESULT,
                title = applicationContext.getString(R.string.variability_mining_success_title),
                text = applicationContext.getString(R.string.variability_mining_success_text),
                isError = false,
            )
            Result.success()
        } catch (t: Throwable) {
            analyticsLogger.logError(t)
            val message = t.message?.takeIf { it.isNotBlank() } ?: t.toString()
            applicationContext.showTitleTextNotification(
                id = NOTIFICATION_ID_RESULT,
                title = applicationContext.getString(R.string.variability_mining_failure_title),
                text = message,
                isError = true,
            )
            Result.failure(
                workDataOf(MEAL_VARIABILITY_MINING_OUTPUT_ERROR to message),
            )
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification =
            NotificationCompat.Builder(applicationContext, CHANNEL_ID_FOREGROUND)
                .setContentTitle(applicationContext.getString(R.string.variability_mining_foreground_title))
                .setSmallIcon(R.drawable.ic_nutrition)
                .setOngoing(true)
                .build()

        return ForegroundInfo(
            FOREGROUND_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 124_010
        private const val NOTIFICATION_ID_RESULT = 124_011

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<MineMealVariabilityWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                MEAL_VARIABILITY_MINING_UNIQUE_WORK,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
