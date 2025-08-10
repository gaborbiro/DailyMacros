package dev.gaborbiro.dailymacros.features.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dev.gaborbiro.dailymacros.data.chatgpt.AuthInterceptor
import dev.gaborbiro.dailymacros.data.chatgpt.ChatGPTRepositoryImpl
import dev.gaborbiro.dailymacros.data.chatgpt.service.ChatGPTService
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.ContentEntryOutputContentDeserializer
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.data.chatgpt.service.model.OutputContentDeserializer
import dev.gaborbiro.dailymacros.data.records.DBMapper
import dev.gaborbiro.dailymacros.data.records.RecordsRepositoryImpl
import dev.gaborbiro.dailymacros.design.DailyMacrosTheme
import dev.gaborbiro.dailymacros.features.common.NutrientsUIMapper
import dev.gaborbiro.dailymacros.features.common.RecordsUIMapper
import dev.gaborbiro.dailymacros.features.modal.ModalActivity.Companion.REQUEST_TIMEOUT_IN_SECONDS
import dev.gaborbiro.dailymacros.features.modal.RecordsMapper
import dev.gaborbiro.dailymacros.features.modal.usecase.FetchNutrientsUseCase
import dev.gaborbiro.dailymacros.features.overview.OverviewNavigatorImpl
import dev.gaborbiro.dailymacros.features.overview.OverviewScreen
import dev.gaborbiro.dailymacros.features.overview.OverviewViewModel
import dev.gaborbiro.dailymacros.features.overview.useCases.ObserveMacroGoalsProgressUseCase
import dev.gaborbiro.dailymacros.store.bitmap.BitmapStore
import dev.gaborbiro.dailymacros.store.db.AppDatabase
import dev.gaborbiro.dailymacros.store.file.FileStoreFactoryImpl
import okhttp3.OkHttpClient
import okhttp3.java.net.cookiejar.JavaNetCookieJar
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.util.concurrent.TimeUnit.SECONDS

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navigator = OverviewNavigatorImpl(this)
        val fileStore = FileStoreFactoryImpl(this).getStore("public", keepFiles = true)
        val bitmapStore = BitmapStore(fileStore)

        val recordsRepository = RecordsRepositoryImpl(
            templatesDAO = AppDatabase.getInstance().templatesDAO(),
            recordsDAO = AppDatabase.getInstance().recordsDAO(),
            dBMapper = DBMapper(),
            bitmapStore = bitmapStore,
        )

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

        val nutrientsUIMapper = NutrientsUIMapper()

        val fetchNutrientsUseCase = FetchNutrientsUseCase(
            appContext = this,
            bitmapStore = bitmapStore,
            recordsRepository = recordsRepository,
            chatGPTRepository = chatGPTRepository,
            recordsMapper = RecordsMapper(),
            nutrientsUIMapper = nutrientsUIMapper,
        )

        val viewModel = OverviewViewModel(
            navigator = navigator,
            repository = recordsRepository,
            uiMapper = RecordsUIMapper(bitmapStore, nutrientsUIMapper),
            fetchNutrientsUseCase = fetchNutrientsUseCase,
            observeMacroGoalsProgressUseCase = ObserveMacroGoalsProgressUseCase(
                recordsRepository = recordsRepository,
            )
        )

        setContent {
            DailyMacrosTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = NoteList.route,
                ) {
                    composable(route = NoteList.route) {
                        OverviewScreen(viewModel)
                    }
                }
            }
        }
    }
}
