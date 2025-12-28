package dev.gaborbiro.dailymacros.features.settings.targets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.features.settings.targets.model.FieldErrors
import dev.gaborbiro.dailymacros.features.settings.targets.model.MacroType
import dev.gaborbiro.dailymacros.features.settings.targets.model.TargetUIModel
import dev.gaborbiro.dailymacros.features.settings.targets.model.TargetsSettingsEvents
import dev.gaborbiro.dailymacros.features.settings.targets.model.TargetsViewState
import dev.gaborbiro.dailymacros.features.settings.targets.model.ValidationError
import dev.gaborbiro.dailymacros.repo.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class TargetsSettingsViewModel(
    private val repo: SettingsRepository,
    private val mapper: TargetsUIMapper = TargetsUIMapper(),
) : ViewModel() {

    private val _viewState = MutableStateFlow(
        TargetsViewState(
            targets = emptyMap(),
        )
    )
    val viewState: StateFlow<TargetsViewState> = _viewState.asStateFlow()
    private val _events = MutableSharedFlow<TargetsSettingsEvents>()
    val events = _events.asSharedFlow()


    private var savedTargets: TargetsViewState? = null

    init {
        load()
    }

    private fun load() {
        val loaded = repo.get()
        val targets = mapper.map(targets = loaded)
        savedTargets = targets
        _viewState.value = targets
    }

    fun onTargetChanged(type: MacroType, target: TargetUIModel) {
        val current = _viewState.value.targets
        val targets = current + (type to target)

        // validate all
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
                // mark both fields, so user knows they conflict
                minErr = ValidationError.MinGreaterThanMax
                maxErr = ValidationError.MinGreaterThanMax
            }

            FieldErrors(minError = minErr, maxError = maxErr)
        }

        val dirty = current != savedTargets
        val updated = TargetsViewState(
            targets = targets,
            canReset = dirty,
            canSave = dirty,
            showExitDialog = _viewState.value.showExitDialog,
            errors = errors,
        )

        _viewState.value = updated
    }

    fun onSaveTapped() {
        val current = _viewState.value
        if (!current.canSave) {
            // Either nothing to save, or errors present
            return
        }

        repo.save(mapper.map(current))
        savedTargets = current

        _viewState.value = current.copy(
            canReset = false,
            canSave = false,
            showExitDialog = false,
            errors = emptyMap()
        )
        viewModelScope.launch {
            _events.emit(TargetsSettingsEvents.Hide)
        }
    }

    fun onUnsavedTargetsDiscardTapped() {
        onTargetsResetTapped()
        _viewState.value = _viewState.value.copy(
            showExitDialog = false,
        )
        viewModelScope.launch {
            _events.emit(TargetsSettingsEvents.Close)
        }
    }

    fun onUnsavedTargetsDismissRequested() {
        _viewState.value = _viewState.value.copy(
            showExitDialog = false,
        )
        viewModelScope.launch {
            _events.emit(TargetsSettingsEvents.Show)
        }
    }

    fun onTargetsResetTapped() {
        val saved = mapper.map(repo.get())
        savedTargets = saved
        _viewState.value = saved
    }

    fun onBottomSheetDismissRequested() {
        val dirty = _viewState.value.canReset
        if (dirty) {
            _viewState.value = _viewState.value.copy(
                showExitDialog = true,
            )
        } else {
            viewModelScope.launch {
                _events.emit(TargetsSettingsEvents.Close)
            }
        }
    }
}