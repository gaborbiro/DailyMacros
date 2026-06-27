package dev.gaborbiro.dailymacros.features.settings.promptEditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gaborbiro.dailymacros.features.settings.promptEditor.model.PromptEditorUiState
import dev.gaborbiro.dailymacros.repositories.chatgpt.ApiKeyValidator
import dev.gaborbiro.dailymacros.repositories.chatgpt.di.ForImageUploadChatGpt
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PromptEditorUiUpdates {
    object Show : PromptEditorUiUpdates()
    object Hide : PromptEditorUiUpdates()
    object Close : PromptEditorUiUpdates()
    data class ShowToast(val message: String) : PromptEditorUiUpdates()
}

@HiltViewModel
class PromptEditorViewModel @Inject constructor(
    @ForImageUploadChatGpt private val chatGPTRepository: ChatGPTRepository,
    private val settingsRepository: SettingsRepository,
    private val apiKeyValidator: ApiKeyValidator,
) : ViewModel() {

    companion object {
        const val TAB_RECOGNITION = "recognition"
        const val TAB_ANALYSIS = "analysis"
        const val TAB_INSIGHTS = "insights"
        const val TAB_ONGOING_INSIGHTS = "ongoing_insights"
        val TAB_TYPES = listOf(TAB_RECOGNITION, TAB_ANALYSIS, TAB_INSIGHTS, TAB_ONGOING_INSIGHTS)
    }

    private val _uiState = MutableStateFlow(PromptEditorUiState())
    val uiState: StateFlow<PromptEditorUiState> = _uiState.asStateFlow()

    private val _uiUpdates = MutableSharedFlow<PromptEditorUiUpdates>()
    val uiUpdates: SharedFlow<PromptEditorUiUpdates> = _uiUpdates.asSharedFlow()

    init {
        val customizations = settingsRepository.getPromptCustomizations()
        val storedKey = settingsRepository.getApiKeyOverride()
        val tabVersions = TAB_TYPES.associateWith { type ->
            settingsRepository.getPromptVersions(type).sortedByDescending { it.version }
        }
        val tabSelectedVersionIndex = TAB_TYPES.associateWith { type ->
            if (tabVersions[type]!!.isNotEmpty()) 1 else 0
        }
        _uiState.value = PromptEditorUiState(
            recognitionSegments = chatGPTRepository.getRecognitionPromptSegments(),
            analysisSegments = chatGPTRepository.getAnalysisPromptSegments(),
            insightsSegments = chatGPTRepository.getInsightsPromptSegments(),
            ongoingInsightsSegments = chatGPTRepository.getOngoingInsightsPromptSegments(),
            currentValues = customizations,
            originalValues = customizations,
            tabVersions = tabVersions,
            tabSelectedVersionIndex = tabSelectedVersionIndex,
            storedApiKeyOverride = storedKey,
            apiKeyDraft = storedKey ?: "",
        )
    }

    fun onValueChanged(segmentId: String, text: String) {
        _uiState.update { it.copy(currentValues = it.currentValues + (segmentId to text)) }
    }

    fun onVersionSelected(tabType: String, index: Int) {
        if (hasUnsavedChanges(tabType)) {
            _uiState.update { it.copy(pendingSwitch = tabType to index, showExitDialog = true) }
        } else {
            applyVersion(tabType, index)
        }
    }

    fun onSaveTapped(tabType: String) {
        val pending = _uiState.value.pendingSwitch
        persistCurrentVersion(tabType)
        _uiState.update { it.copy(showExitDialog = false, pendingSwitch = null) }
        if (pending != null) {
            applyVersion(pending.first, pending.second)
        } else {
            viewModelScope.launch { _uiUpdates.emit(PromptEditorUiUpdates.Hide) }
        }
    }

    fun onBottomSheetDismissRequested() {
        if (_uiState.value.hasAnyUnsavedChanges) {
            _uiState.update { it.copy(showExitDialog = true) }
        } else {
            viewModelScope.launch { _uiUpdates.emit(PromptEditorUiUpdates.Close) }
        }
    }

    fun onExitDialogSaveTapped() {
        val state = _uiState.value
        val pending = state.pendingSwitch
        // Save all tabs that have unsaved changes
        TAB_TYPES.forEach { tabType ->
            if (hasUnsavedChanges(tabType)) {
                persistCurrentVersion(tabType)
            }
        }
        _uiState.update { it.copy(showExitDialog = false, pendingSwitch = null) }
        if (pending != null) {
            applyVersion(pending.first, pending.second)
        } else {
            viewModelScope.launch { _uiUpdates.emit(PromptEditorUiUpdates.Close) }
        }
    }

    fun onExitDialogDiscardTapped() {
        val pending = _uiState.value.pendingSwitch
        val original = _uiState.value.originalValues
        _uiState.update {
            it.copy(currentValues = original, showExitDialog = false, pendingSwitch = null)
        }
        if (pending != null) {
            applyVersion(pending.first, pending.second)
        } else {
            viewModelScope.launch { _uiUpdates.emit(PromptEditorUiUpdates.Close) }
        }
    }

    fun onExitDialogDismissed() {
        _uiState.update { it.copy(showExitDialog = false, pendingSwitch = null) }
        viewModelScope.launch { _uiUpdates.emit(PromptEditorUiUpdates.Show) }
    }

    fun onDeleteVersion(tabType: String, index: Int) {
        val realIndex = index - 1
        val state = _uiState.value
        val versions = state.tabVersions[tabType] ?: return
        val versionToDelete = versions.getOrNull(realIndex) ?: return
        settingsRepository.deletePromptVersion(versionToDelete.version)
        val updatedVersions = versions.toMutableList().also { it.removeAt(realIndex) }
        val currentSelected = state.tabSelectedVersionIndex[tabType] ?: 0
        val newSelected = when {
            updatedVersions.isEmpty() -> 0
            currentSelected == index -> 1.coerceAtMost(updatedVersions.size)
            currentSelected > index -> currentSelected - 1
            else -> currentSelected
        }
        _uiState.update {
            it.copy(
                tabVersions = it.tabVersions + (tabType to updatedVersions),
                tabSelectedVersionIndex = it.tabSelectedVersionIndex + (tabType to newSelected),
            )
        }
        applyVersion(tabType, newSelected)
    }

    fun onApiKeyDraftChanged(text: String) {
        _uiState.update { it.copy(apiKeyDraft = text) }
    }

    fun onUnlockTapped() {
        val draft = _uiState.value.apiKeyDraft.trim()
        if (draft.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUnlocking = true) }
            val valid = apiKeyValidator.validate(draft)
            _uiState.update { it.copy(isUnlocking = false) }
            if (valid) {
                settingsRepository.setApiKeyOverride(draft)
                _uiState.update { it.copy(storedApiKeyOverride = draft) }
                _uiUpdates.emit(PromptEditorUiUpdates.ShowToast("API key verified and saved. Your key will be used for future AI queries."))
            } else {
                _uiUpdates.emit(PromptEditorUiUpdates.ShowToast("Invalid API key. Please check it and try again."))
            }
        }
    }

    fun onClearApiKeyTapped() {
        settingsRepository.clearApiKeyOverride()
        settingsRepository.clearPromptCustomizations()
        _uiState.update { it.withApiKeyCleared() }
        viewModelScope.launch {
            _uiUpdates.emit(PromptEditorUiUpdates.ShowToast("API key removed. The default key will be used."))
        }
    }

    private fun hasUnsavedChanges(tabType: String): Boolean {
        val ids = segmentIdsForTab(tabType)
        val state = _uiState.value
        return ids.any { state.currentValues[it] != state.originalValues[it] }
    }

    private fun segmentIdsForTab(tabType: String): Set<String> {
        val state = _uiState.value
        val segments = when (tabType) {
            TAB_RECOGNITION -> state.recognitionSegments
            TAB_ANALYSIS -> state.analysisSegments
            TAB_INSIGHTS -> state.insightsSegments
            TAB_ONGOING_INSIGHTS -> state.ongoingInsightsSegments
            else -> emptyList()
        }
        return segments.filterIsInstance<PromptSegment.Editable>().map { it.id }.toSet()
    }

    private fun persistCurrentVersion(tabType: String) {
        val ids = segmentIdsForTab(tabType)
        val tabValues = _uiState.value.currentValues.filterKeys { it in ids }.filterValues { it.isNotBlank() }
        val newVersion = settingsRepository.savePromptVersion(tabType, tabValues)
        val newVersions = settingsRepository.getPromptVersions(tabType).sortedByDescending { it.version }
        val newIndex = newVersions.indexOfFirst { it.version == newVersion.version } + 1
        val mergedCustomizations = settingsRepository.getPromptCustomizations() + tabValues
        settingsRepository.setPromptCustomizations(mergedCustomizations)
        _uiState.update {
            it.copy(
                originalValues = it.originalValues + tabValues,
                currentValues = it.currentValues + tabValues,
                tabVersions = it.tabVersions + (tabType to newVersions),
                tabSelectedVersionIndex = it.tabSelectedVersionIndex + (tabType to newIndex),
            )
        }
    }

    private fun applyVersion(tabType: String, index: Int) {
        val ids = segmentIdsForTab(tabType)
        if (index == 0) {
            val mergedCustomizations = settingsRepository.getPromptCustomizations() - ids
            settingsRepository.setPromptCustomizations(mergedCustomizations)
            _uiState.update {
                val clearedValues = it.currentValues - ids
                val clearedOriginal = it.originalValues - ids
                it.copy(
                    currentValues = clearedValues,
                    originalValues = clearedOriginal,
                    tabSelectedVersionIndex = it.tabSelectedVersionIndex + (tabType to 0),
                )
            }
        } else {
            val version = _uiState.value.tabVersions[tabType]?.getOrNull(index - 1) ?: return
            val mergedCustomizations = settingsRepository.getPromptCustomizations() + version.customizations
            settingsRepository.setPromptCustomizations(mergedCustomizations)
            _uiState.update {
                it.copy(
                    currentValues = it.currentValues + version.customizations,
                    originalValues = it.originalValues + version.customizations,
                    tabSelectedVersionIndex = it.tabSelectedVersionIndex + (tabType to index),
                )
            }
        }
    }
}
