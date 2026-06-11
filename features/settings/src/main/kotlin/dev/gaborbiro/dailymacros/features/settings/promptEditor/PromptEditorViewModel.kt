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
        val saved = settingsRepository.getPromptCustomizations()
        _uiState.value = PromptEditorUiState(
            recognitionSegments = chatGPTRepository.getRecognitionPromptSegments(),
            analysisSegments = chatGPTRepository.getAnalysisPromptSegments(),
            currentValues = saved,
            originalValues = saved,
        )
    }

    fun onValueChanged(segmentId: String, text: String) {
        _uiState.update { it.copy(currentValues = it.currentValues + (segmentId to text)) }
    }

    fun onResetSegment(segmentId: String) {
        _uiState.update { it.copy(currentValues = it.currentValues - segmentId) }
    }

    fun onSaveTapped() {
        val values = _uiState.value.currentValues.filterValues { it.isNotBlank() }
        settingsRepository.setPromptCustomizations(values)
        _uiState.update { it.copy(originalValues = values, currentValues = values, showExitDialog = false) }
        viewModelScope.launch { _uiUpdates.emit(PromptEditorUiUpdates.Hide) }
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
        val original = _uiState.value.originalValues
        _uiState.update { it.copy(currentValues = original, showExitDialog = false) }
        viewModelScope.launch { _uiUpdates.emit(PromptEditorUiUpdates.Close) }
    }

    fun onExitDialogDismissed() {
        _uiState.update { it.copy(showExitDialog = false) }
        viewModelScope.launch { _uiUpdates.emit(PromptEditorUiUpdates.Show) }
    }
}
