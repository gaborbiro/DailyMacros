package dev.gaborbiro.dailymacros.features.settings.model

sealed class SettingsUiUpdates {
    data object NavigateBack : SettingsUiUpdates()
    data object RestartApplication : SettingsUiUpdates()
    data class ShowSnackbar(val message: String) : SettingsUiUpdates()
    data object RequestGoogleSignIn : SettingsUiUpdates()
    data object RequestPhotoPermissions : SettingsUiUpdates()
    data class RestoreConfirmNeeded(val modifiedAtMs: Long, val fileId: String) : SettingsUiUpdates()
}
