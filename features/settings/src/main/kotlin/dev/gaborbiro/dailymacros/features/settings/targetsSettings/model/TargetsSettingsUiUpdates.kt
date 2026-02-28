package dev.gaborbiro.dailymacros.features.settings.targetsSettings.model

sealed class TargetsSettingsUiUpdates {
    data object Hide : TargetsSettingsUiUpdates()
    data object Close : TargetsSettingsUiUpdates()
    data object Show : TargetsSettingsUiUpdates()
}
