package dev.gaborbiro.nutri.features.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dev.gaborbiro.nutri.data.chatgpt.AuthInterceptor
import dev.gaborbiro.nutri.data.chatgpt.ChatGPTRepositoryImpl
import dev.gaborbiro.nutri.data.chatgpt.service.ChatGPTService
import dev.gaborbiro.nutri.data.chatgpt.service.model.ContentEntry
import dev.gaborbiro.nutri.data.chatgpt.service.model.ContentEntryOutputContentDeserializer
import dev.gaborbiro.nutri.data.chatgpt.service.model.OutputContent
import dev.gaborbiro.nutri.data.chatgpt.service.model.OutputContentDeserializer
import dev.gaborbiro.nutri.data.records.domain.RecordsRepository
import dev.gaborbiro.nutri.design.NotesTheme
import dev.gaborbiro.nutri.features.common.RecordsMapper
import dev.gaborbiro.nutri.features.modal.ModalActivity.Companion.REQUEST_TIMEOUT_IN_SECONDS
import dev.gaborbiro.nutri.features.modal.usecase.FetchNutrientsUseCase
import dev.gaborbiro.nutri.features.overview.OverviewNavigatorImpl
import dev.gaborbiro.nutri.features.overview.OverviewScreen
import dev.gaborbiro.nutri.features.overview.OverviewViewModel
import dev.gaborbiro.nutri.store.bitmap.BitmapStore
import dev.gaborbiro.nutri.store.file.FileStoreFactoryImpl
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
        val recordsRepository = RecordsRepository.get(fileStore)

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

        val fetchNutrientsUseCase = FetchNutrientsUseCase(
            appContext = this,
            bitmapStore = bitmapStore,
            recordsRepository = recordsRepository,
            chatGPTRepository = chatGPTRepository,
            mapper = RecordsMapper(),
        )

        val viewModel = OverviewViewModel(
            appContext = this,
            navigator = navigator,
            fetchNutrientsUseCase = fetchNutrientsUseCase,
        )

        setContent {
            NotesTheme {
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
