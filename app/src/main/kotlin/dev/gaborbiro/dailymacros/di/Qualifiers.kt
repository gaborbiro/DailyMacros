package dev.gaborbiro.dailymacros.di

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicPersistentFileStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicEphemeralFileStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ForImageUploadChatGpt

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ForJsonBodyChatGpt

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ChatGptClientGson
