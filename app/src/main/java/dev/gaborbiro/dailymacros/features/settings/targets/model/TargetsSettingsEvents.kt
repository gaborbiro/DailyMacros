package dev.gaborbiro.dailymacros.features.settings.targets.model

internal sealed class TargetsSettingsEvents {
    data object Hide : TargetsSettingsEvents()
    data object Close : TargetsSettingsEvents()
    data object Show : TargetsSettingsEvents()
}