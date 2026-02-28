package dev.gaborbiro.dailymacros.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiState
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ExportFoodDiaryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val navigator: SettingsNavigator,
    private val appInfo: SettingsAppInfo,
    private val exportFoodDiaryUseCase: ExportFoodDiaryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            showTargetsSettings = false,
            bottomLabel = appInfo.versionLabel,
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onBackNavigateRequested() {
        _uiState.value = SettingsUiState(
            showTargetsSettings = false,
            bottomLabel = appInfo.versionLabel,
        )
        navigator.navigateBack()
    }

    fun onTargetsSettingsTapped() {
        _uiState.value = _uiState.value.copy(
            showTargetsSettings = true,
        )
    }

    fun onTargetsSettingsCloseRequested() {
        _uiState.value = _uiState.value.copy(
            showTargetsSettings = false,
        )
    }

    fun onExportSettingsTapped() {
        viewModelScope.launch {
            exportFoodDiaryUseCase.execute()
        }
    }
}
