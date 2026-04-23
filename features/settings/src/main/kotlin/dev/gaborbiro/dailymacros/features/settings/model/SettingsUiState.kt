package dev.gaborbiro.dailymacros.features.settings.model

data class SettingsUiState(
    val showTargetsSettings: Boolean,
    val bottomLabel: String,
    val variabilityMiningLoading: Boolean = false,
    val variabilityMiningError: String? = null,
    val variabilityMiningRequestJson: String? = null,
    val variabilityMiningResponseJson: String? = null,
)
