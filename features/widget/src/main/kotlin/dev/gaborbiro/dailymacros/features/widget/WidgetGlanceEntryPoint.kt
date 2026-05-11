package dev.gaborbiro.dailymacros.features.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetGlanceEntryPoint {
    fun widgetGlanceDependencies(): WidgetGlanceDependencies
}
