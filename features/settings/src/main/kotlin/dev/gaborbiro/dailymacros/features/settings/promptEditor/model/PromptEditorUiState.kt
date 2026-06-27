package dev.gaborbiro.dailymacros.features.settings.promptEditor.model

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.PromptSegment
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion

data class PromptEditorUiState(
    val recognitionSegments: List<PromptSegment> = emptyList(),
    val analysisSegments: List<PromptSegment> = emptyList(),
    val insightsSegments: List<PromptSegment> = emptyList(),
    val ongoingInsightsSegments: List<PromptSegment> = emptyList(),
    val currentValues: Map<String, String> = emptyMap(),
    val originalValues: Map<String, String> = emptyMap(),
    /** Versions per prompt type key (see PromptEditorViewModel.TAB_* constants). */
    val tabVersions: Map<String, List<PromptVersion>> = emptyMap(),
    /** Selected version index per tab type. 0 = v0 (defaults), 1+ = saved versions. */
    val tabSelectedVersionIndex: Map<String, Int> = emptyMap(),
    /** Pending version switch (tabType to targetVersionIndex) blocked by unsaved changes. */
    val pendingSwitch: Pair<String, Int>? = null,
    val showExitDialog: Boolean = false,
    val storedApiKeyOverride: String? = null,
    val apiKeyDraft: String = "",
    val isUnlocking: Boolean = false,
    val aiInsightsEnabled: Boolean = false,
) {
    val hasAnyUnsavedChanges: Boolean get() = currentValues != originalValues
    val isApiKeyOverridden: Boolean get() = storedApiKeyOverride != null
    val promptsEnabled: Boolean get() = isApiKeyOverridden

    fun withApiKeyCleared(): PromptEditorUiState = copy(
        storedApiKeyOverride = null,
        apiKeyDraft = "",
        currentValues = emptyMap(),
        originalValues = emptyMap(),
        tabSelectedVersionIndex = tabSelectedVersionIndex.mapValues { 0 },
    )
}
