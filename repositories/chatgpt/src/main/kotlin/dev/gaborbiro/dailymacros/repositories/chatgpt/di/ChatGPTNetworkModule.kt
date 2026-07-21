package dev.gaborbiro.dailymacros.repositories.chatgpt.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.repositories.chatgpt.AuthInterceptor
import dev.gaborbiro.dailymacros.repositories.chatgpt.ChatGPTMapper
import dev.gaborbiro.dailymacros.repositories.chatgpt.ChatGPTRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.chatgpt.ChatGptOkHttpTimeouts
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ClientIdProvider
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGptClientGson
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ForImageUploadChatGpt
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ForJsonBodyChatGpt
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.ChatGPTService
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ContentEntry
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ContentEntryOutputContentDeserializer
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.OutputContent
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.OutputContentDeserializer
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import okhttp3.OkHttpClient
import okhttp3.java.net.cookiejar.JavaNetCookieJar
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ChatGPTNetworkModule {

    @Provides
    @Singleton
    @ChatGptClientGson
    fun chatGptGson(): Gson =
        GsonBuilder()
            .registerTypeAdapter(OutputContent::class.java, OutputContentDeserializer())
            .registerTypeAdapter(
                object : TypeToken<ContentEntry<OutputContent>>() {}.type,
                ContentEntryOutputContentDeserializer(),
            )
            .create()

    @Provides
    @Singleton
    @ForImageUploadChatGpt
    fun imageUploadOkHttp(
        settingsRepository: SettingsRepository,
        clientIdProvider: ClientIdProvider,
    ): OkHttpClient {
        val logger = HttpLoggingInterceptor().also { it.level = HttpLoggingInterceptor.Level.BODY }
        val authInterceptor = AuthInterceptor(settingsRepository, FirebaseAuth.getInstance(), clientIdProvider)
        return OkHttpClient.Builder()
            .addNetworkInterceptor(logger)
            .addInterceptor(authInterceptor)
            .addNetworkInterceptor(authInterceptor)
            .also { ChatGptOkHttpTimeouts.applyImageUploadTimeouts(it) }
            .cookieJar(JavaNetCookieJar(CookieManager()))
            .build()
    }

    @Provides
    @Singleton
    @ForJsonBodyChatGpt
    fun jsonBodyOkHttp(
        settingsRepository: SettingsRepository,
        clientIdProvider: ClientIdProvider,
    ): OkHttpClient {
        val logger = HttpLoggingInterceptor().also { it.level = HttpLoggingInterceptor.Level.BODY }
        val authInterceptor = AuthInterceptor(settingsRepository, FirebaseAuth.getInstance(), clientIdProvider)
        return OkHttpClient.Builder()
            .addNetworkInterceptor(logger)
            .addInterceptor(authInterceptor)
            .addNetworkInterceptor(authInterceptor)
            .also { ChatGptOkHttpTimeouts.applyJsonBodyTimeouts(it) }
            .cookieJar(JavaNetCookieJar(CookieManager()))
            .build()
    }

    @Provides
    @Singleton
    @ForImageUploadChatGpt
    fun imageUploadRetrofit(
        @ForImageUploadChatGpt client: OkHttpClient,
        @ChatGptClientGson gson: Gson,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @ForJsonBodyChatGpt
    fun jsonBodyRetrofit(
        @ForJsonBodyChatGpt client: OkHttpClient,
        @ChatGptClientGson gson: Gson,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @ForImageUploadChatGpt
    fun imageUploadChatGptRepository(
        @ForImageUploadChatGpt retrofit: Retrofit,
        mapper: ChatGPTMapper,
        settingsRepository: SettingsRepository,
    ): ChatGPTRepository =
        ChatGPTRepositoryImpl(
            service = retrofit.create(ChatGPTService::class.java),
            mapper = mapper,
            settingsRepository = settingsRepository,
        )

    @Provides
    @Singleton
    @ForJsonBodyChatGpt
    fun jsonBodyChatGptRepository(
        @ForJsonBodyChatGpt retrofit: Retrofit,
        mapper: ChatGPTMapper,
        settingsRepository: SettingsRepository,
    ): ChatGPTRepository =
        ChatGPTRepositoryImpl(
            service = retrofit.create(ChatGPTService::class.java),
            mapper = mapper,
            settingsRepository = settingsRepository,
        )
}
