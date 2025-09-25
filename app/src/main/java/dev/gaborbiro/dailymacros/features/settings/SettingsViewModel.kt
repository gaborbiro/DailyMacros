package dev.gaborbiro.dailymacros.features.settings

import androidx.lifecycle.ViewModel
import dev.gaborbiro.dailymacros.BuildConfig
import dev.gaborbiro.dailymacros.features.common.AppPrefs
import dev.gaborbiro.dailymacros.features.settings.model.FieldErrors
import dev.gaborbiro.dailymacros.features.settings.model.MacroType
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUIModel
import dev.gaborbiro.dailymacros.features.settings.model.SettingsViewState
import dev.gaborbiro.dailymacros.features.settings.model.TargetUIModel
import dev.gaborbiro.dailymacros.features.settings.model.ValidationError
import dev.gaborbiro.dailymacros.repo.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class SettingsViewModel(
    private val navigator: SettingsNavigator,
    private val repo: SettingsRepository,
    private val mapper: SettingsUIMapper = SettingsUIMapper(),
    private val appPrefs: AppPrefs,
) : ViewModel() {

    private val _viewState = MutableStateFlow(
        SettingsViewState(
            settings = SettingsUIModel(targets = emptyMap()),
            bottomLabel = bottomLabel,
        )
    )
    val viewState: StateFlow<SettingsViewState> = _viewState.asStateFlow()

    private var savedSettings: SettingsUIModel? = null

    init {
        load()
    }

    private fun load() {
        val loaded = repo.loadTargets()
        val uiModel = mapper.map(loaded)
        savedSettings = uiModel
        _viewState.value = SettingsViewState(
            settings = uiModel,
            canReset = false,
            canSave = false,
            bottomLabel = bottomLabel,
        )
    }

    fun onMacroTargetChange(type: MacroType, target: TargetUIModel) {
        val current = _viewState.value.settings
        val updated = current.copy(targets = current.targets + (type to target))

        // validate all
        val errors = updated.targets.mapValues { (_, t) ->
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

        val dirty = updated != savedSettings

        _viewState.value = SettingsViewState(
            settings = updated,
            canReset = dirty,
            canSave = dirty,
            showExitDialog = _viewState.value.showExitDialog,
            errors = errors,
            bottomLabel = bottomLabel,
        )
    }

    fun save() {
        val state = _viewState.value
        if (!state.canSave) {
            // Either nothing to save, or errors present
            return
        }

        val current = state.settings
        repo.save(mapper.map(current))
        savedSettings = current

        _viewState.value = state.copy(
            canReset = false,
            canSave = false,
            showExitDialog = false,
            errors = emptyMap()
        )
    }

    fun reset() {
        val saved = mapper.map(repo.loadTargets())
        savedSettings = saved
        _viewState.value = SettingsViewState(
            settings = saved,
            canReset = false,
            canSave = false,
            bottomLabel = bottomLabel,
        )
    }

    fun onBackClick() {
        val dirty = _viewState.value.canReset
        if (dirty) {
            _viewState.value = _viewState.value.copy(showExitDialog = true)
        } else {
            navigator.navigateBack()
        }
    }

    fun discardAndExit() {
        reset()
        navigator.navigateBack()
    }

    fun dismissExitDialog() {
        _viewState.value = _viewState.value.copy(showExitDialog = false)
    }

    private val bottomLabel: String
        get() {
            return "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})  |  UserID: ${appPrefs.userUUID}"
        }
}
