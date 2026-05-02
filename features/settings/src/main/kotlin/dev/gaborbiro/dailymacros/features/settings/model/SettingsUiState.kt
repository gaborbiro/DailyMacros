package dev.gaborbiro.dailymacros.features.settings.model

data class SettingsUiState(
    val showTargetsSettings: Boolean,
    val bottomLabel: String,
    val showBackupProgressIndicator: Boolean = false,
    val variabilityMiningLoading: Boolean = false,
    val variabilityMiningError: String? = null,
    val variabilityMiningRequestJson: String? = null,
    val variabilityMiningResponseJson: String? = null,
    val variabilityMiningGeneratedAt: String? = null,
    /** Preorder 0/1 expansion flags for the request JSON tree viewer. */
    val variabilityMiningRequestJsonExpansionBits: String = "",
    /** Preorder 0/1 expansion flags for the response JSON tree viewer. */
    val variabilityMiningResponseJsonExpansionBits: String = "",
    val variabilityMiningRequestJsonSectionExpanded: Boolean = false,
    val variabilityMiningResponseJsonSectionExpanded: Boolean = false,
)
