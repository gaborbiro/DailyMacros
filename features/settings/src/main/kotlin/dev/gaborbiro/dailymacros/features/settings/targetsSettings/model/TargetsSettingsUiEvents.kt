package dev.gaborbiro.dailymacros.features.settings.targetsSettings.model

sealed class TargetsSettingsUiEvents {
    data object Hide : TargetsSettingsUiEvents()
    data object Close : TargetsSettingsUiEvents()
    data object Show : TargetsSettingsUiEvents()
}
