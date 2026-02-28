package dev.gaborbiro.dailymacros.features.settings.targets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.features.settings.targets.model.FieldErrors
import dev.gaborbiro.dailymacros.features.settings.targets.model.MacroType
import dev.gaborbiro.dailymacros.features.settings.targets.model.TargetUIModel
import dev.gaborbiro.dailymacros.features.settings.targets.model.TargetsSettingsUiEvents
import dev.gaborbiro.dailymacros.features.settings.targets.model.TargetsUiState
import dev.gaborbiro.dailymacros.features.settings.targets.model.ValidationError
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TargetsSettingsViewModel(
    private val repo: SettingsRepository,
    private val mapper: TargetsUIMapper = TargetsUIMapper(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TargetsUiState(
            targets = emptyMap(),
        )
    )
    val uiState: StateFlow<TargetsUiState> = _uiState.asStateFlow()
    private val _uiEvents = MutableSharedFlow<TargetsSettingsUiEvents>()
    val uiEvents = _uiEvents.asSharedFlow()


    private var savedTargets: TargetsUiState? = null

    init {
        load()
    }

    private fun load() {
        val loaded = repo.getTargets()
        val targets = mapper.map(targets = loaded)
        savedTargets = targets
        _uiState.value = targets
    }

    fun onTargetChanged(type: MacroType, target: TargetUIModel) {
        val current = _uiState.value.targets
        val targets = current + (type to target)

        val errors = targets.mapValues { (_, t) ->
            var minErr: ValidationError? = null
            var maxErr: ValidationError? = null

            if (t.min == null) {
                minErr = ValidationError.Empty
            }
            if (t.max == null) {
                maxErr = ValidationError.Empty
            }
            if (t.min != null && t.max != null && t.min > t.max) {
                minErr = ValidationError.MinGreaterThanMax
                maxErr = ValidationError.MinGreaterThanMax
            }

            FieldErrors(minError = minErr, maxError = maxErr)
        }

        val dirty = current != savedTargets
        val updated = TargetsUiState(
            targets = targets,
            canReset = dirty,
            canSave = dirty,
            showExitDialog = _uiState.value.showExitDialog,
            errors = errors,
        )

        _uiState.value = updated
    }

    fun onSaveTapped() {
        val current = _uiState.value
        if (!current.canSave) {
            return
        }

        repo.setTargets(mapper.map(current))
        savedTargets = current

        _uiState.value = current.copy(
            canReset = false,
            canSave = false,
            showExitDialog = false,
            errors = emptyMap()
        )
        viewModelScope.launch {
            _uiEvents.emit(TargetsSettingsUiEvents.Hide)
        }
    }

    fun onUnsavedTargetsDiscardTapped() {
        onTargetsResetTapped()
        _uiState.value = _uiState.value.copy(
            showExitDialog = false,
        )
        viewModelScope.launch {
            _uiEvents.emit(TargetsSettingsUiEvents.Close)
        }
    }

    fun onUnsavedTargetsDismissRequested() {
        _uiState.value = _uiState.value.copy(
            showExitDialog = false,
        )
        viewModelScope.launch {
            _uiEvents.emit(TargetsSettingsUiEvents.Show)
        }
    }

    fun onTargetsResetTapped() {
        val saved = mapper.map(repo.getTargets())
        savedTargets = saved
        _uiState.value = saved
    }

    fun onBottomSheetDismissRequested() {
        val dirty = _uiState.value.canReset
        if (dirty) {
            _uiState.value = _uiState.value.copy(
                showExitDialog = true,
            )
        } else {
            viewModelScope.launch {
                _uiEvents.emit(TargetsSettingsUiEvents.Close)
            }
        }
    }
}
