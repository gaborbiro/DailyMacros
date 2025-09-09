package dev.gaborbiro.dailymacros.features.settings

import dev.gaborbiro.dailymacros.features.settings.model.TargetUIModel
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUIModel
import dev.gaborbiro.dailymacros.features.settings.model.SettingsViewState
import dev.gaborbiro.dailymacros.repo.settings.SettingsRepository
import dev.gaborbiro.dailymacros.features.settings.model.MacroType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class SettingsViewModel(
    private val navigator: SettingsNavigator,
    private val repo: SettingsRepository,
    private val mapper: UIMapper = UIMapper(),
) {
    private val _viewState: MutableStateFlow<SettingsViewState> =
        MutableStateFlow(SettingsViewState(settings = SettingsUIModel(targets = emptyMap())))
    val viewState: StateFlow<SettingsViewState> = _viewState.asStateFlow()

    private var savedSettings: SettingsUIModel? = null

    init {
        load()
    }

    private fun load() {
        val loaded = repo.loadTargets()
        val uiModel = mapper.map(loaded)
        savedSettings = uiModel
        _viewState.value = SettingsViewState(settings = uiModel, canReset = false)
    }

    fun onMacroTargetChange(type: MacroType, target: TargetUIModel) {
        if (target.min <= target.max) {
            val current = _viewState.value.settings
            val updated: SettingsUIModel =
                current.copy(targets = current.targets + (type to target))
            repo.save(mapper.map(updated))

            _viewState.value = SettingsViewState(
                settings = updated,
                canReset = updated != savedSettings
            )
        } else {
        }
    }

    fun reset() {
        savedSettings?.let { saved ->
            repo.save(mapper.map(saved))
            _viewState.value = SettingsViewState(settings = saved, canReset = false)
        }
    }

    fun onBackClick() {
        navigator.navigateBack()
    }
}
