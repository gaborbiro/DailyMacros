package dev.gaborbiro.dailymacros.features.settings.targets.model

internal sealed class TargetsEvents {
    data object Hide : TargetsEvents()
    data object Close : TargetsEvents()
    data object Show : TargetsEvents()
}