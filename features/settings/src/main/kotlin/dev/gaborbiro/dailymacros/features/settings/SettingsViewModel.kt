package dev.gaborbiro.dailymacros.features.settings

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ExportFoodDiaryUseCase
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiState
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiUpdates
import dev.gaborbiro.dailymacros.features.settings.variability.MineMealVariabilityPreviewUseCase
import dev.gaborbiro.dailymacros.repositories.records.domain.VariabilityRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class SettingsViewModel(
    private val application: Application,
    appInfo: SettingsAppInfo,
    private val settingsPrefs: SettingsPrefs,
    private val exportFoodDiaryUseCase: ExportFoodDiaryUseCase,
    private val mineMealVariabilityPreviewUseCase: MineMealVariabilityPreviewUseCase,
    private val variabilityRepository: VariabilityRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            showTargetsSettings = false,
            bottomLabel = appInfo.versionLabel,
            variabilityMiningRequestJson = settingsPrefs.variabilityMiningRequestJson,
            variabilityMiningResponseJson = settingsPrefs.variabilityMiningResponseJson,
            variabilityMiningGeneratedAt = generatedAtDisplayLine(
                settingsPrefs.variabilityMiningGeneratedAtEpochMs.takeIf { it > 0L },
            ),
            variabilityMiningRequestJsonExpansionBits = settingsPrefs.variabilityMiningRequestJsonExpansionBits,
            variabilityMiningResponseJsonExpansionBits = settingsPrefs.variabilityMiningResponseJsonExpansionBits,
            variabilityMiningRequestJsonSectionExpanded = settingsPrefs.variabilityMiningRequestJsonSectionExpanded,
            variabilityMiningResponseJsonSectionExpanded = settingsPrefs.variabilityMiningResponseJsonSectionExpanded,
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
                    val generatedAt = System.currentTimeMillis()
                    settingsPrefs.variabilityMiningRequestJson = preview.requestJsonPretty
                    settingsPrefs.variabilityMiningResponseJson = preview.responseJsonPretty
                    settingsPrefs.variabilityMiningGeneratedAtEpochMs = generatedAt
                    settingsPrefs.variabilityMiningRequestJsonExpansionBits = ""
                    settingsPrefs.variabilityMiningResponseJsonExpansionBits = ""
                    settingsPrefs.variabilityMiningRequestJsonSectionExpanded = false
                    settingsPrefs.variabilityMiningResponseJsonSectionExpanded = false
                    _uiState.update {
                        it.copy(
                            variabilityMiningLoading = false,
                            variabilityMiningRequestJson = preview.requestJsonPretty,
                            variabilityMiningResponseJson = preview.responseJsonPretty,
                            variabilityMiningGeneratedAt =
                                generatedAtDisplayLine(generatedAt),
                            variabilityMiningRequestJsonExpansionBits = "",
                            variabilityMiningResponseJsonExpansionBits = "",
                            variabilityMiningRequestJsonSectionExpanded = false,
                            variabilityMiningResponseJsonSectionExpanded = false,
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

    fun onClearVariabilityProfileTapped() {
        viewModelScope.launch {
            runCatching {
                variabilityRepository.clearProfile()
                settingsPrefs.clearVariabilityMiningDebugCache()
            }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            variabilityMiningRequestJson = null,
                            variabilityMiningResponseJson = null,
                            variabilityMiningGeneratedAt = null,
                            variabilityMiningRequestJsonExpansionBits = "",
                            variabilityMiningResponseJsonExpansionBits = "",
                            variabilityMiningRequestJsonSectionExpanded = false,
                            variabilityMiningResponseJsonSectionExpanded = false,
                            variabilityMiningError = null,
                        )
                    }
                    _uiUpdates.emit(SettingsUiUpdates.ShowSnackbar("Meal variability profile cleared"))
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(variabilityMiningError = t.message ?: t.toString())
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

    fun onVariabilityMiningRequestJsonExpansionBitsChange(bits: String) {
        settingsPrefs.variabilityMiningRequestJsonExpansionBits = bits
        _uiState.update { it.copy(variabilityMiningRequestJsonExpansionBits = bits) }
    }

    fun onVariabilityMiningResponseJsonExpansionBitsChange(bits: String) {
        settingsPrefs.variabilityMiningResponseJsonExpansionBits = bits
        _uiState.update { it.copy(variabilityMiningResponseJsonExpansionBits = bits) }
    }

    fun onVariabilityMiningRequestJsonSectionExpandedChange(expanded: Boolean) {
        settingsPrefs.variabilityMiningRequestJsonSectionExpanded = expanded
        _uiState.update { it.copy(variabilityMiningRequestJsonSectionExpanded = expanded) }
    }

    fun onVariabilityMiningResponseJsonSectionExpandedChange(expanded: Boolean) {
        settingsPrefs.variabilityMiningResponseJsonSectionExpanded = expanded
        _uiState.update { it.copy(variabilityMiningResponseJsonSectionExpanded = expanded) }
    }

    private fun copyJsonToClipboard(text: String) {
        val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("application/json", text))
    }

    private fun generatedAtDisplayLine(epochMs: Long?, zoneId: ZoneId = ZoneId.systemDefault()): String? {
        val ms = epochMs ?: return null
        if (ms <= 0L) return null
        val formatted = Instant.ofEpochMilli(ms)
            .atZone(zoneId)
            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
        return "Generated at: $formatted"
    }
}
