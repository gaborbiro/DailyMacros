package dev.gaborbiro.dailymacros.features.settings

sealed class SettingsUiEvents {
    data object NavigateBack : SettingsUiEvents()
}
