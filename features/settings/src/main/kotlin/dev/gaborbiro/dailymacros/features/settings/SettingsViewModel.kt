package dev.gaborbiro.dailymacros.features.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ExportFoodDiaryUseCase
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ExportSqliteDatabaseUseCase
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ImportSqliteDatabaseResult
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ImportSqliteDatabaseUseCase
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiState
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
    application: Application,
    appInfo: SettingsAppInfo,
    private val exportFoodDiaryUseCase: ExportFoodDiaryUseCase,
    private val exportSqliteDatabaseUseCase: ExportSqliteDatabaseUseCase,
    private val importSqliteDatabaseUseCase: ImportSqliteDatabaseUseCase,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            showTargetsSettings = false,
            bottomLabel = appInfo.versionLabel,
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiUpdates = MutableSharedFlow<SettingsUiUpdates>()
    val uiUpdates: SharedFlow<SettingsUiUpdates> = _uiUpdates.asSharedFlow()

    fun onBackNavigateRequested() {
        _uiState.update {
            it.copy(showTargetsSettings = false)
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

    fun onExportDbTapped() {
        viewModelScope.launch {
            _uiState.update { it.copy(exportDataInProgress = true) }
            runCatching { exportSqliteDatabaseUseCase.execute() }
                .onFailure { t ->
                    _uiUpdates.emit(
                        SettingsUiUpdates.ShowSnackbar(t.message ?: t.toString()),
                    )
                }
            _uiState.update { it.copy(exportDataInProgress = false) }
        }
    }

    fun onImportDbTapped() {
        viewModelScope.launch {
            _uiState.update { it.copy(importDataInProgress = true) }
            when (val result = importSqliteDatabaseUseCase.execute()) {
                ImportSqliteDatabaseResult.Cancelled -> Unit
                ImportSqliteDatabaseResult.InvalidFile ->
                    _uiUpdates.emit(
                        SettingsUiUpdates.ShowSnackbar("That file is not a valid backup (.tar)"),
                    )

                ImportSqliteDatabaseResult.RestartPending -> Unit
                is ImportSqliteDatabaseResult.Error ->
                    _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar(result.message))
            }
            _uiState.update { it.copy(importDataInProgress = false) }
        }
    }
}
