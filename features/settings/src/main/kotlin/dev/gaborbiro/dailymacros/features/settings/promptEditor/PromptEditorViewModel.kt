package dev.gaborbiro.dailymacros.features.settings.promptEditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gaborbiro.dailymacros.features.settings.promptEditor.model.PromptEditorUiState
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
}

@HiltViewModel
class PromptEditorViewModel @Inject constructor(
    @ForImageUploadChatGpt private val chatGPTRepository: ChatGPTRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PromptEditorUiState())
    val uiState: StateFlow<PromptEditorUiState> = _uiState.asStateFlow()

    private val _uiUpdates = MutableSharedFlow<PromptEditorUiUpdates>()
    val uiUpdates: SharedFlow<PromptEditorUiUpdates> = _uiUpdates.asSharedFlow()

    init {
        val versions = settingsRepository.getPromptVersions().sortedByDescending { it.version }
        val selectedIndex = if (versions.isNotEmpty()) 0 else -1
        val customizations = when {
            selectedIndex >= 0 -> versions[selectedIndex].customizations
            else -> settingsRepository.getPromptCustomizations()
        }
        _uiState.value = PromptEditorUiState(
            recognitionSegments = chatGPTRepository.getRecognitionPromptSegments(),
            analysisSegments = chatGPTRepository.getAnalysisPromptSegments(),
            currentValues = customizations,
            originalValues = customizations,
            versions = versions,
            selectedVersionIndex = selectedIndex,
        )
    }

    fun onValueChanged(segmentId: String, text: String) {
        _uiState.update { it.copy(currentValues = it.currentValues + (segmentId to text)) }
    }

    fun onResetTab(segmentIds: List<String>) {
        _uiState.update { it.copy(currentValues = it.currentValues - segmentIds.toSet()) }
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
        val state = _uiState.value
        val versionToDelete = state.versions.getOrNull(index) ?: return
        settingsRepository.deletePromptVersion(versionToDelete.version)
        val updatedVersions = state.versions.toMutableList().also { it.removeAt(index) }
        val newSelectedIndex = when {
            updatedVersions.isEmpty() -> -1
            state.selectedVersionIndex == index -> 0.coerceAtMost(updatedVersions.lastIndex)
            state.selectedVersionIndex > index -> state.selectedVersionIndex - 1
            else -> state.selectedVersionIndex
        }
        val newCustomizations = updatedVersions.getOrNull(newSelectedIndex)?.customizations ?: emptyMap()
        _uiState.update {
            it.copy(
                versions = updatedVersions,
                selectedVersionIndex = newSelectedIndex,
                currentValues = newCustomizations,
                originalValues = newCustomizations,
            )
        }
    }

    private fun persistCurrentVersion() {
        val values = _uiState.value.currentValues.filterValues { it.isNotBlank() }
        val newVersion = settingsRepository.savePromptVersion(values)
        val newVersions = settingsRepository.getPromptVersions().sortedByDescending { it.version }
        val newIndex = newVersions.indexOfFirst { it.version == newVersion.version }
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
        val customizations = _uiState.value.versions.getOrNull(index)?.customizations ?: emptyMap()
        _uiState.update {
            it.copy(
                currentValues = customizations,
                originalValues = customizations,
                selectedVersionIndex = index,
            )
        }
    }
}
