package dev.gaborbiro.nutrition.data.chatgpt.service.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.nutrition.data.chatgpt.service.ChatGPTService
import retrofit2.Retrofit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
internal class NetworkModule {

    @Provides
    @Singleton
    fun provideChatGPTService(@WithSession retrofit: Retrofit): ChatGPTService {
        return retrofit
            .create(ChatGPTService::class.java)
    }
}