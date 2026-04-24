package dev.gaborbiro.dailymacros.features.settings

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ExportFoodDiaryUseCase
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiState
import dev.gaborbiro.dailymacros.features.settings.variability.MineMealVariabilityPreviewUseCase
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiUpdates
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val application: Application,
    private val appInfo: SettingsAppInfo,
    private val settingsPrefs: SettingsPrefs,
    private val exportFoodDiaryUseCase: ExportFoodDiaryUseCase,
    private val mineMealVariabilityPreviewUseCase: MineMealVariabilityPreviewUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            showTargetsSettings = false,
            bottomLabel = appInfo.versionLabel,
            variabilityMiningRequestJson = settingsPrefs.variabilityMiningRequestJson,
            variabilityMiningResponseJson = settingsPrefs.variabilityMiningResponseJson,
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiUpdates = MutableSharedFlow<SettingsUiUpdates>()
    val uiUpdates: SharedFlow<SettingsUiUpdates> = _uiUpdates.asSharedFlow()

    fun onBackNavigateRequested() {
        _uiState.update {
            it.copy(
                showTargetsSettings = false,
                variabilityMiningLoading = false,
                variabilityMiningError = null,
            )
        }
        viewModelScope.launch {
            _uiUpdates.emit(SettingsUiUpdates.NavigateBack)
        }
    }

    fun onTargetsSettingsTapped() {
        _uiState.update {
            it.copy(showTargetsSettings = true)
        }
    }

    fun onTargetsSettingsCloseRequested() {
        _uiState.update {
            it.copy(showTargetsSettings = false)
        }
    }

    fun onExportSettingsTapped() {
        viewModelScope.launch {
            exportFoodDiaryUseCase.execute()
        }
    }

    fun onVariabilityMiningPreviewTapped() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    variabilityMiningLoading = true,
                    variabilityMiningError = null,
                )
            }
            runCatching { mineMealVariabilityPreviewUseCase.execute() }
                .onSuccess { preview ->
                    settingsPrefs.variabilityMiningRequestJson = preview.requestJsonPretty
                    settingsPrefs.variabilityMiningResponseJson = preview.responseJsonPretty
                    _uiState.update {
                        it.copy(
                            variabilityMiningLoading = false,
                            variabilityMiningRequestJson = preview.requestJsonPretty,
                            variabilityMiningResponseJson = preview.responseJsonPretty,
                        )
                    }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            variabilityMiningLoading = false,
                            variabilityMiningError = t.message ?: t.toString(),
                        )
                    }
                }
        }
    }

    fun onCopyVariabilityRequestJson() {
        val text = _uiState.value.variabilityMiningRequestJson ?: return
        copyJsonToClipboard(text)
        viewModelScope.launch {
            _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Request JSON copied"))
        }
    }

    fun onCopyVariabilityResponseJson() {
        val text = _uiState.value.variabilityMiningResponseJson ?: return
        copyJsonToClipboard(text)
        viewModelScope.launch {
            _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Response JSON copied"))
        }
    }

    private fun copyJsonToClipboard(text: String) {
        val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("application/json", text))
    }
}
