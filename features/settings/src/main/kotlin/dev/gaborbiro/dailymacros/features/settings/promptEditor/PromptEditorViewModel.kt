package dev.gaborbiro.dailymacros.features.settings.promptEditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gaborbiro.dailymacros.features.settings.promptEditor.model.PromptEditorUiState
import dev.gaborbiro.dailymacros.repositories.chatgpt.ApiKeyValidator
import dev.gaborbiro.dailymacros.repositories.chatgpt.di.ForImageUploadChatGpt
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
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

    private val _uiState = MutableStateFlow(PromptEditorUiState())
    val uiState: StateFlow<PromptEditorUiState> = _uiState.asStateFlow()

    private val _uiUpdates = MutableSharedFlow<PromptEditorUiUpdates>()
    val uiUpdates: SharedFlow<PromptEditorUiUpdates> = _uiUpdates.asSharedFlow()

    init {
        // Index 0 is always the synthetic v0 (default). User-saved versions start at index 1.
        val versions = settingsRepository.getPromptVersions().sortedByDescending { it.version }
        val selectedIndex = if (versions.isNotEmpty()) 1 else 0
        val customizations = versions.getOrNull(selectedIndex - 1)?.customizations ?: emptyMap()
        val storedKey = settingsRepository.getApiKeyOverride()
        _uiState.value = PromptEditorUiState(
            recognitionSegments = chatGPTRepository.getRecognitionPromptSegments(),
            analysisSegments = chatGPTRepository.getAnalysisPromptSegments(),
            insightsSegments = chatGPTRepository.getInsightsPromptSegments(),
            ongoingInsightsSegments = chatGPTRepository.getOngoingInsightsPromptSegments(),
            currentValues = customizations,
            originalValues = customizations,
            versions = versions,
            selectedVersionIndex = selectedIndex,
            storedApiKeyOverride = storedKey,
            apiKeyDraft = storedKey ?: "",
        )
    }

    fun onValueChanged(segmentId: String, text: String) {
        _uiState.update { it.copy(currentValues = it.currentValues + (segmentId to text)) }
    }

    fun onVersionSelected(index: Int) {
        if (_uiState.value.hasUnsavedChanges) {
            _uiState.update { it.copy(pendingVersionIndex = index, showExitDialog = true) }
        } else {
            applyVersion(index)
        }
    }

    fun onSaveTapped() {
        val pending = _uiState.value.pendingVersionIndex
        persistCurrentVersion()
        _uiState.update { it.copy(showExitDialog = false, pendingVersionIndex = null) }
        if (pending != null) {
            applyVersion(pending)
        } else {
            viewModelScope.launch { _uiUpdates.emit(PromptEditorUiUpdates.Hide) }
        }
    }

    fun onBottomSheetDismissRequested() {
        if (_uiState.value.hasUnsavedChanges) {
            _uiState.update { it.copy(showExitDialog = true) }
        } else {
            viewModelScope.launch { _uiUpdates.emit(PromptEditorUiUpdates.Close) }
        }
    }

    fun onExitDialogSaveTapped() {
        onSaveTapped()
    }

    fun onExitDialogDiscardTapped() {
        val pending = _uiState.value.pendingVersionIndex
        val original = _uiState.value.originalValues
        _uiState.update {
            it.copy(currentValues = original, showExitDialog = false, pendingVersionIndex = null)
        }
        if (pending != null) {
            applyVersion(pending)
        } else {
            viewModelScope.launch { _uiUpdates.emit(PromptEditorUiUpdates.Close) }
        }
    }

    fun onExitDialogDismissed() {
        _uiState.update { it.copy(showExitDialog = false, pendingVersionIndex = null) }
        viewModelScope.launch { _uiUpdates.emit(PromptEditorUiUpdates.Show) }
    }

    fun onDeleteVersion(index: Int) {
        // index is 1-based (0 is v0 which is never deletable)
        val realIndex = index - 1
        val state = _uiState.value
        val versionToDelete = state.versions.getOrNull(realIndex) ?: return
        settingsRepository.deletePromptVersion(versionToDelete.version)
        val updatedVersions = state.versions.toMutableList().also { it.removeAt(realIndex) }
        val newSelectedIndex = when {
            updatedVersions.isEmpty() -> 0
            state.selectedVersionIndex == index -> 1.coerceAtMost(updatedVersions.size)
            state.selectedVersionIndex > index -> state.selectedVersionIndex - 1
            else -> state.selectedVersionIndex
        }
        val newCustomizations = updatedVersions.getOrNull(newSelectedIndex - 1)?.customizations ?: emptyMap()
        _uiState.update {
            it.copy(
                versions = updatedVersions,
                selectedVersionIndex = newSelectedIndex,
                currentValues = newCustomizations,
                originalValues = newCustomizations,
            )
        }
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

    private fun persistCurrentVersion() {
        val values = _uiState.value.currentValues.filterValues { it.isNotBlank() }
        val newVersion = settingsRepository.savePromptVersion(values)
        val newVersions = settingsRepository.getPromptVersions().sortedByDescending { it.version }
        // +1 because index 0 is always v0
        val newIndex = newVersions.indexOfFirst { it.version == newVersion.version } + 1
        _uiState.update {
            it.copy(
                originalValues = values,
                currentValues = values,
                versions = newVersions,
                selectedVersionIndex = newIndex,
            )
        }
    }

    private fun applyVersion(index: Int) {
        if (index == 0) {
            settingsRepository.clearPromptCustomizations()
            _uiState.update { it.withV0Applied() }
        } else {
            val customizations = _uiState.value.versions.getOrNull(index - 1)?.customizations ?: emptyMap()
            _uiState.update {
                it.copy(
                    currentValues = customizations,
                    originalValues = customizations,
                    selectedVersionIndex = index,
                )
            }
        }
    }
}
