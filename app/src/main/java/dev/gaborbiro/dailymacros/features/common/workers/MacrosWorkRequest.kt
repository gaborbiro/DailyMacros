package dev.gaborbiro.dailymacros.features.common.workers

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.data.db.AppDatabase
import dev.gaborbiro.dailymacros.data.file.FileStoreFactoryImpl
import dev.gaborbiro.dailymacros.data.image.ImageStoreImpl
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.features.common.DateUIMapper
import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.modal.ModalActivity.Companion.REQUEST_TIMEOUT_IN_SECONDS
import dev.gaborbiro.dailymacros.features.modal.RecordsMapper
import dev.gaborbiro.dailymacros.features.modal.usecase.FetchMacrosUseCase
import dev.gaborbiro.dailymacros.repo.chatgpt.AuthInterceptor
import dev.gaborbiro.dailymacros.repo.chatgpt.ChatGPTRepositoryImpl
import dev.gaborbiro.dailymacros.repo.chatgpt.service.ChatGPTService
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.ContentEntryOutputContentDeserializer
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repo.chatgpt.service.model.OutputContentDeserializer
import dev.gaborbiro.dailymacros.repo.records.ApiMapper
import dev.gaborbiro.dailymacros.repo.records.RecordsRepositoryImpl
import dev.gaborbiro.dailymacros.util.CHANNEL_ID_FOREGROUND
import okhttp3.OkHttpClient
import okhttp3.java.net.cookiejar.JavaNetCookieJar
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.util.concurrent.TimeUnit.SECONDS

class MacrosWorkRequest(
    appContext: Context,
    private val workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {

    private val imageStore: ImageStore by lazy {
        val fileStore = FileStoreFactoryImpl(appContext).getStore("public", keepFiles = true)
        ImageStoreImpl(fileStore)
    }

    private val recordsRepository by lazy {
        RecordsRepositoryImpl(
            templatesDAO = AppDatabase.getInstance().templatesDAO(),
            recordsDAO = AppDatabase.getInstance().recordsDAO(),
            mapper = ApiMapper(),
            imageStore = imageStore,
        )
    }

    private val fetchMacrosUseCase: FetchMacrosUseCase by lazy {
        val logger = HttpLoggingInterceptor().also {
            it.level = HttpLoggingInterceptor.Level.BODY
        }
        val authInterceptor = AuthInterceptor()
        val builder = OkHttpClient.Builder()
            .addNetworkInterceptor(logger)
            .addInterceptor(authInterceptor)
            .addNetworkInterceptor(authInterceptor)
            .callTimeout(REQUEST_TIMEOUT_IN_SECONDS, SECONDS)
            .connectTimeout(REQUEST_TIMEOUT_IN_SECONDS, SECONDS)
            .readTimeout(REQUEST_TIMEOUT_IN_SECONDS, SECONDS)
            .writeTimeout(REQUEST_TIMEOUT_IN_SECONDS, SECONDS)

        val okHttpClient = builder
            .cookieJar(JavaNetCookieJar(CookieManager()))
            .build()

        val gson = GsonBuilder()
            .registerTypeAdapter(OutputContent::class.java, OutputContentDeserializer())
            .registerTypeAdapter(
                object : TypeToken<ContentEntry<OutputContent>>() {}.type,
                ContentEntryOutputContentDeserializer()
            )
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val chatGPTRepository = ChatGPTRepositoryImpl(
            service = retrofit.create(ChatGPTService::class.java)
        )
        val dateUIMapper = DateUIMapper()
        FetchMacrosUseCase(
            appContext = appContext,
            imageStore = imageStore,
            chatGPTRepository = chatGPTRepository,
            recordsRepository = recordsRepository,
            recordsMapper = RecordsMapper(),
            macrosUIMapper = MacrosUIMapper(dateUIMapper),
        )
    }

    companion object {
        private const val PREFS_RECORD_ID_KEY = "record_id"

        fun getWorkRequest(
            recordId: Long,
        ): WorkRequest {
            return OneTimeWorkRequestBuilder<MacrosWorkRequest>()
                .setInputData(
                    Data.Builder()
                        .putLong(PREFS_RECORD_ID_KEY, recordId)
                        .build()
                )
                .build()
        }
    }

    override suspend fun doWork(): Result {
        return try {
            setForeground(createForegroundInfo())
            val recordId =
                workerParameters.inputData.getLong(
                    PREFS_RECORD_ID_KEY, -1L
                )
            if (recordId == -1L) {
                Result.failure()
            } else {
                fetchMacrosUseCase.execute(recordId)
                Result.success()
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            Result.failure()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification =
            NotificationCompat.Builder(applicationContext, CHANNEL_ID_FOREGROUND)
                .setContentTitle("Fetching macros…")
                .setSmallIcon(R.drawable.ic_nutrition)
                .setOngoing(true)
                .build()

        return ForegroundInfo(
            /* notificationId = */ 1,
            /* notification = */ notification,
            /* foregroundServiceType = */ ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }
}
