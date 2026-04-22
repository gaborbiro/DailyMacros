package dev.gaborbiro.dailymacros.features.settings.targetsSettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.FieldErrors
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.MacroType
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.TargetUiModel
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.TargetsSettingsUiUpdates
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.TargetsSettingsUiState
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.model.ValidationError
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TargetsSettingsViewModel(
    private val repo: SettingsRepository,
    private val uiMapper: TargetsSettingsUiMapper = TargetsSettingsUiMapper(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(TargetsSettingsUiState())
    val uiState: StateFlow<TargetsSettingsUiState> = _uiState.asStateFlow()

    private val _uiUpdates = MutableSharedFlow<TargetsSettingsUiUpdates>()
    val uiUpdates = _uiUpdates.asSharedFlow()

    private var savedTargets: TargetsSettingsUiState? = null

    init {
        load()
    }

    private fun load() {
        val loaded = repo.getTargets()
        val targets = uiMapper.map(targets = loaded)
        savedTargets = targets
        _uiState.value = targets
    }

    /**
     * Reloads persisted targets into UI state. Use when opening the sheet from another screen
     * so edits made elsewhere are visible.
     */
    fun reloadFromRepository() {
        load()
    }

    fun onTargetChanged(type: MacroType, target: TargetUiModel) {
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
        val updated = TargetsSettingsUiState(
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

        repo.setTargets(uiMapper.map(current))
        savedTargets = current

        _uiState.value = current.copy(
            canReset = false,
            canSave = false,
            showExitDialog = false,
            errors = emptyMap()
        )
        viewModelScope.launch {
            _uiUpdates.emit(TargetsSettingsUiUpdates.Hide)
        }
    }

    fun onUnsavedTargetsDiscardTapped() {
        onTargetsResetTapped()
        _uiState.update {
            it.copy(showExitDialog = false)
        }
        viewModelScope.launch {
            _uiUpdates.emit(TargetsSettingsUiUpdates.Close)
        }
    }

    fun onUnsavedTargetsDismissRequested() {
        _uiState.update {
            it.copy(showExitDialog = false)
        }
        viewModelScope.launch {
            _uiUpdates.emit(TargetsSettingsUiUpdates.Show)
        }
    }

    fun onTargetsResetTapped() {
        val saved = uiMapper.map(repo.getTargets())
        savedTargets = saved
        _uiState.value = saved
    }

    fun onBottomSheetDismissRequested() {
        val dirty = _uiState.value.canReset
        if (dirty) {
            _uiState.update {
                it.copy(showExitDialog = true)
            }
        } else {
            viewModelScope.launch {
                _uiUpdates.emit(TargetsSettingsUiUpdates.Close)
            }
        }
    }
}
