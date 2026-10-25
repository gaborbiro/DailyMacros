package dev.gaborbiro.dailymacros.features.shared.di

import javax.inject.Qualifier

/**
 * A [kotlinx.coroutines.CoroutineScope] that lives as long as the app process, on the main
 * thread. For work started from a screen that must finish even after the screen has closed.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
