package dev.gaborbiro.dailymacros.features.settings.model

sealed class SettingsUiUpdates {
    data object NavigateBack : SettingsUiUpdates()
    data object RestartApplication : SettingsUiUpdates()
    data class ShowSnackbar(val message: String) : SettingsUiUpdates()
    data object RequestGoogleSignIn : SettingsUiUpdates()
}
