package dev.gaborbiro.dailymacros.features.settings.promptEditor.model

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion

data class PromptEditorUiState(
    val recognitionSegments: List<PromptSegment> = emptyList(),
    val analysisSegments: List<PromptSegment> = emptyList(),
    val currentValues: Map<String, String> = emptyMap(),
    val originalValues: Map<String, String> = emptyMap(),
    val showExitDialog: Boolean = false,
    val versions: List<PromptVersion> = emptyList(),
    val selectedVersionIndex: Int = -1,
    val pendingVersionIndex: Int? = null,
    val storedApiKeyOverride: String? = null,
    val apiKeyDraft: String = "",
    val isUnlocking: Boolean = false,
) {
    val hasUnsavedChanges: Boolean get() = currentValues != originalValues
    val canSave: Boolean get() = hasUnsavedChanges
    val isApiKeyOverridden: Boolean get() = storedApiKeyOverride != null
    val promptsEnabled: Boolean get() = isApiKeyOverridden

    /** All prompt/version state that belongs to v0 (defaults). */
    fun withV0Applied(): PromptEditorUiState = copy(
        selectedVersionIndex = 0,
        currentValues = emptyMap(),
        originalValues = emptyMap(),
    )

    /** Full reset when the API key is cleared — v0 state plus key fields. */
    fun withApiKeyCleared(): PromptEditorUiState = withV0Applied().copy(
        storedApiKeyOverride = null,
        apiKeyDraft = "",
    )
}
