package dev.gaborbiro.dailymacros.features.settings.targets.model

import dev.gaborbiro.dailymacros.features.settings.targets.model.MacroType

internal data class TargetsViewState(
    val targets: Map<MacroType, TargetUIModel>,
    val canReset: Boolean = false,
    val canSave: Boolean = false,
    val showExitDialog: Boolean = false,
    val errors: Map<MacroType, FieldErrors> = emptyMap(),
)

internal data class TargetUIModel(
    val enabled: Boolean,
    val min: Int?,  // calories in cal, others in g
    val max: Int?,
    val theoreticalMax: Int,
)

data class FieldErrors(
    val minError: ValidationError? = null,
    val maxError: ValidationError? = null
)

sealed interface ValidationError {
    object Empty : ValidationError
    object MinGreaterThanMax : ValidationError
}
