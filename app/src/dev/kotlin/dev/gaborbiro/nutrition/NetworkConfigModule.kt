package dev.gaborbiro.nutrition

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.nutrition.network.di.BaseUrl
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class NetworkConfigModule {

    companion object {
        private const val BASE_URL = "https://api.openai.com/"
    }

    @Provides
    @BaseUrl
    @Singleton
    fun provideBaseURL(): String {
        return BASE_URL
    }
}