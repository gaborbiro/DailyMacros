package dev.gaborbiro.dailymacros.repositories.chatgpt.domain

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ForImageUploadChatGpt

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ForJsonBodyChatGpt
