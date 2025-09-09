package dev.gaborbiro.dailymacros.features.settings.model


internal data class SettingsUIModel(
    val targets: Map<MacroType, TargetUIModel>,
)

internal data class TargetUIModel(
    val enabled: Boolean = true,
    val min: Int?,  // calories in cal, others in g
    val max: Int?,
    val theoreticalMax: Int,
)
