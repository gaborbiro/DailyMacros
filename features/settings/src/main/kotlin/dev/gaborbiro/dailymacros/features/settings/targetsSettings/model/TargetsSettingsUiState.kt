package dev.gaborbiro.dailymacros.features.settings.targetsSettings.model

data class TargetsSettingsUiState(
    val targets: Map<MacroType, TargetUiModel> = emptyMap(),
    val canReset: Boolean = false,
    val canSave: Boolean = false,
    val showExitDialog: Boolean = false,
    val errors: Map<MacroType, FieldErrors> = emptyMap(),
)

data class TargetUiModel(
    val enabled: Boolean,
    val min: Int?,
    val max: Int?,
    val theoreticalMax: Int,
)

data class FieldErrors(
    val minError: ValidationError? = null,
    val maxError: ValidationError? = null
)

sealed interface ValidationError {
    data object Empty : ValidationError
    data object MinGreaterThanMax : ValidationError
}
