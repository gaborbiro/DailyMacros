package dev.gaborbiro.dailymacros.features.settings.model

internal data class SettingsViewState(
    val settings: SettingsUIModel,
    val canReset: Boolean = false,
    val canSave: Boolean = false,
    val showExitDialog: Boolean = false,
    val errors: Map<MacroType, FieldErrors> = emptyMap()
)

data class FieldErrors(
    val minError: ValidationError? = null,
    val maxError: ValidationError? = null
)

sealed interface ValidationError {
    object Empty : ValidationError
    object MinGreaterThanMax : ValidationError
}
