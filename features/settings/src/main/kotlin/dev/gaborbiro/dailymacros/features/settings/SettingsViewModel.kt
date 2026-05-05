package dev.gaborbiro.dailymacros.features.settings

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ExportFoodDiaryUseCase
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ExportSqliteDatabaseUseCase
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ImportSqliteDatabaseResult
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ImportSqliteDatabaseUseCase
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiState
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiUpdates
import dev.gaborbiro.dailymacros.features.settings.variability.MEAL_VARIABILITY_MINING_OUTPUT_ERROR
import dev.gaborbiro.dailymacros.features.settings.variability.MEAL_VARIABILITY_MINING_UNIQUE_WORK
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
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
    private val exportSqliteDatabaseUseCase: ExportSqliteDatabaseUseCase,
    private val importSqliteDatabaseUseCase: ImportSqliteDatabaseUseCase,
    private val variabilityRepository: VariabilityRepository,
    private val recordsRepository: RecordsRepository,
    private val enqueueMealVariabilityMining: () -> Unit,
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
            nextMineTemplateCount = null,
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiUpdates = MutableSharedFlow<SettingsUiUpdates>()
    val uiUpdates: SharedFlow<SettingsUiUpdates> = _uiUpdates.asSharedFlow()

    init {
        viewModelScope.launch {
            WorkManager.getInstance(application)
                .getWorkInfosForUniqueWorkFlow(MEAL_VARIABILITY_MINING_UNIQUE_WORK)
                .collect { infos ->
                    val info = infos.firstOrNull() ?: return@collect
                    when (info.state) {
                        WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING ->
                            _uiState.update {
                                it.copy(
                                    variabilityMiningLoading = true,
                                    variabilityMiningError = null,
                                )
                            }

                        WorkInfo.State.SUCCEEDED -> {
                            _uiState.update {
                                it.copy(
                                    variabilityMiningLoading = false,
                                    variabilityMiningError = null,
                                    variabilityMiningRequestJson = settingsPrefs.variabilityMiningRequestJson,
                                    variabilityMiningResponseJson = settingsPrefs.variabilityMiningResponseJson,
                                    variabilityMiningGeneratedAt = generatedAtDisplayLine(
                                        settingsPrefs.variabilityMiningGeneratedAtEpochMs.takeIf { it > 0L },
                                    ),
                                    variabilityMiningRequestJsonExpansionBits =
                                        settingsPrefs.variabilityMiningRequestJsonExpansionBits,
                                    variabilityMiningResponseJsonExpansionBits =
                                        settingsPrefs.variabilityMiningResponseJsonExpansionBits,
                                    variabilityMiningRequestJsonSectionExpanded =
                                        settingsPrefs.variabilityMiningRequestJsonSectionExpanded,
                                    variabilityMiningResponseJsonSectionExpanded =
                                        settingsPrefs.variabilityMiningResponseJsonSectionExpanded,
                                )
                            }
                            viewModelScope.launch {
                                runCatching {
                                    val snap = variabilityRepository.getLatestProfile()
                                    snap?.let { s ->
                                        recordsRepository.countTemplatesPendingVariabilityAfterWatermark(
                                            s.templatesIngestWatermarkEpochMs,
                                        )
                                    }
                                }
                                    .onSuccess { countOrNull ->
                                        _uiState.update { s -> s.copy(nextMineTemplateCount = countOrNull) }
                                    }
                                    .onFailure { t ->
                                        Log.w(TAG, "nextMineTemplateCount after mining", t)
                                        _uiUpdates.emit(
                                            SettingsUiUpdates.ShowSnackbar(
                                                "Could not refresh template count for the next mine.",
                                            ),
                                        )
                                    }
                            }
                        }

                        WorkInfo.State.FAILED -> {
                            val message = info.outputData.getString(MEAL_VARIABILITY_MINING_OUTPUT_ERROR)
                                ?: "Meal variability mining failed"
                            _uiState.update {
                                it.copy(
                                    variabilityMiningLoading = false,
                                    variabilityMiningError = message,
                                )
                            }
                        }

                        WorkInfo.State.CANCELLED, WorkInfo.State.BLOCKED ->
                            _uiState.update { it.copy(variabilityMiningLoading = false) }
                    }
                }
        }
    }

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

    fun refreshTemplateCountForSettings() {
        viewModelScope.launch {
            runCatching {
                variabilityRepository.getLatestProfile()?.let { s ->
                    recordsRepository.countTemplatesPendingVariabilityAfterWatermark(
                        s.templatesIngestWatermarkEpochMs,
                    )
                }
            }
                .onSuccess { countOrNull ->
                    _uiState.update { it.copy(nextMineTemplateCount = countOrNull) }
                }
                .onFailure { t ->
                    Log.w(TAG, "refreshTemplateCountForSettings", t)
                    _uiUpdates.emit(
                        SettingsUiUpdates.ShowSnackbar(
                            "Could not refresh template count for the next mine.",
                        ),
                    )
                }
        }
    }

    fun onVariabilityMiningPreviewTapped() {
        enqueueMealVariabilityMining()
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
                            nextMineTemplateCount = null,
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

    private companion object {
        private const val TAG = "SettingsViewModel"
    }
}
