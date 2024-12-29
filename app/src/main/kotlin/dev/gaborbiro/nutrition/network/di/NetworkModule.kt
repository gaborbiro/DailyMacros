package dev.gaborbiro.nutrition.network.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.nutrition.data.chatgpt.service.di.WithSession
import dev.gaborbiro.nutrition.network.AuthInterceptor
import okhttp3.CookieJar
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Singleton

private val Context.cookiesPrefs: DataStore<Preferences> by preferencesDataStore("cookiePrefs")


@Module
@InstallIn(SingletonComponent::class)
internal class NetworkModule {

    companion object {
        private const val REQUEST_TIMEOUT_IN_SECONDS = 90L
    }

    @Provides
    @Singleton
    @WithSession
    fun providesOkHttpClient(): OkHttpClient.Builder {
        val logger = HttpLoggingInterceptor().also {
            it.level = HttpLoggingInterceptor.Level.BODY
        }
        val authInterceptor = AuthInterceptor()
        return OkHttpClient.Builder()
            .addNetworkInterceptor(logger)
            .addInterceptor(authInterceptor)
            .addNetworkInterceptor(authInterceptor)
            .callTimeout(REQUEST_TIMEOUT_IN_SECONDS, SECONDS)
            .connectTimeout(REQUEST_TIMEOUT_IN_SECONDS, SECONDS)
            .readTimeout(REQUEST_TIMEOUT_IN_SECONDS, SECONDS)
            .writeTimeout(REQUEST_TIMEOUT_IN_SECONDS, SECONDS)
    }

    @Provides
    @Singleton
    @WithSession
    fun providesRetrofit(
        @WithSession okHttpClientBuilder: OkHttpClient.Builder,
        @BaseUrl baseUrl: String,
    ): Retrofit {
        val okHttpClient = okHttpClientBuilder
            .cookieJar(JavaNetCookieJar(CookieManager()))
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}