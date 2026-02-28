package dev.gaborbiro.dailymacros.features.settings.targets.model

internal sealed class TargetsSettingsUiEvents {
    data object Hide : TargetsSettingsUiEvents()
    data object Close : TargetsSettingsUiEvents()
    data object Show : TargetsSettingsUiEvents()
}