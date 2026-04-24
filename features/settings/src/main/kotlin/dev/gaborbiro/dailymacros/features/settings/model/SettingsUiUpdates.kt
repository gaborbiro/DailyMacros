package dev.gaborbiro.dailymacros.features.settings.model

sealed class SettingsUiUpdates {
    data object NavigateBack : SettingsUiUpdates()
    data class ShowSnackbar(val message: String) : SettingsUiUpdates()
}