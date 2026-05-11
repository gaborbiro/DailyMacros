package dev.gaborbiro.dailymacros.di

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention
import javax.inject.Qualifier

/**
 * [FileStoreFactoryImpl] logical bucket name `"public"` with `keepFiles = true`.
 *
 * This is **app-private storage** under the app’s internal files directory (not a shared/public
 * directory like Downloads). The `"public"` segment is only the logical key passed to the factory.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FileStorePublicBucketPersistent

/**
 * Same logical `"public"` bucket as [FileStorePublicBucketPersistent], but backed by the app’s
 * internal cache directory (`keepFiles = false`).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FileStorePublicBucketEphemeral

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ForImageUploadChatGpt

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ForJsonBodyChatGpt

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ChatGptClientGson
