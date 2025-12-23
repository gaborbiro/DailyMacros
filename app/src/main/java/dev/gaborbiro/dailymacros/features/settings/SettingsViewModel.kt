package dev.gaborbiro.dailymacros.features.settings

import androidx.lifecycle.ViewModel
import dev.gaborbiro.dailymacros.BuildConfig
import dev.gaborbiro.dailymacros.features.common.AppPrefs
import dev.gaborbiro.dailymacros.features.settings.model.SettingsViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class SettingsViewModel(
    private val navigator: SettingsNavigator,
    private val appPrefs: AppPrefs,
) : ViewModel() {

    private val _viewState = MutableStateFlow(
        SettingsViewState(
            showTargetsSettings = false,
            bottomLabel = bottomLabel,
        )
    )
    val viewState: StateFlow<SettingsViewState> = _viewState.asStateFlow()

    fun onBackNavigateRequested() {
        _viewState.value = SettingsViewState(
            showTargetsSettings = false,
            bottomLabel = bottomLabel,
        )
        navigator.navigateBack()
    }

    fun onTargetsSettingsTapped() {
        _viewState.value = _viewState.value.copy(
            showTargetsSettings = true,
        )
    }

    fun onTargetsSettingsCloseRequested() {
        _viewState.value = _viewState.value.copy(
            showTargetsSettings = false,
        )
    }

    fun onExportSettingsTapped() {

    }

    private val bottomLabel: String
        get() {
            return "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})  |  UserID: ${appPrefs.userUUID}"
        }
}
