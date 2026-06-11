package dev.gaborbiro.dailymacros.features.settings.promptEditor.model

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment

data class PromptEditorUiState(
    val recognitionSegments: List<PromptSegment> = emptyList(),
    val analysisSegments: List<PromptSegment> = emptyList(),
    val currentValues: Map<String, String> = emptyMap(),
    val originalValues: Map<String, String> = emptyMap(),
    val showExitDialog: Boolean = false,
) {
    val hasUnsavedChanges: Boolean get() = currentValues != originalValues
    val canSave: Boolean get() = hasUnsavedChanges
}
