package dev.gaborbiro.dailymacros.features.widgets.diary

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DiaryGlanceEntryPoint {
    fun widgetGlanceDependencies(): DiaryGlanceDependencies
}
