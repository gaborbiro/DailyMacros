package dev.gaborbiro.dailymacros.features.settings.model

internal data class SettingsViewState(
    val settings: SettingsUIModel,
    val canReset: Boolean = false,
)
