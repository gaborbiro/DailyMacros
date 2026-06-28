package dev.gaborbiro.dailymacros.repositories.chatgpt.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ForJsonBodyChatGpt

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ChatGptClientGson
