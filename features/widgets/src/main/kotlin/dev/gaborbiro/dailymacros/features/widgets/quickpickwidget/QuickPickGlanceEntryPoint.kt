package dev.gaborbiro.dailymacros.features.widgets.quickpickwidget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface QuickPickGlanceEntryPoint {
    fun quickPickGlanceDependencies(): QuickPickGlanceDependencies
}
