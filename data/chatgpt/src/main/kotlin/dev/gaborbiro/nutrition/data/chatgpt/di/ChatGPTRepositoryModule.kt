package dev.gaborbiro.nutrition.data.chatgpt.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dev.gaborbiro.nutrition.data.chatgpt.ChatGPTRepositoryImpl
import dev.gaborbiro.nutrition.data.chatgpt.domain.ChatGPTRepository


@Module
@InstallIn(ViewModelComponent::class)
internal abstract class ChatGPTRepositoryModule {

    @Binds
    abstract fun bindChatGptRepository(impl: ChatGPTRepositoryImpl): ChatGPTRepository
}