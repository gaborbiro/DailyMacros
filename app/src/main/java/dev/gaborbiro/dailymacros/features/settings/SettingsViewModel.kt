package dev.gaborbiro.dailymacros.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.BuildConfig
import dev.gaborbiro.dailymacros.features.common.AppPrefs
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiState
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ExportFoodDiaryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SettingsViewModel(
    private val navigator: SettingsNavigator,
    private val appPrefs: AppPrefs,
    private val exportFoodDiaryUseCase: ExportFoodDiaryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            showTargetsSettings = false,
            bottomLabel = bottomLabel,
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onBackNavigateRequested() {
        _uiState.value = SettingsUiState(
            showTargetsSettings = false,
            bottomLabel = bottomLabel,
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

    private val bottomLabel: String
        get() {
            return "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})  |  UserID: ${appPrefs.userUUID}"
        }
}
