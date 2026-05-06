package dev.gaborbiro.dailymacros.features.settings.model

data class SettingsUiState(
    val showTargetsSettings: Boolean,
    val bottomLabel: String,
    val exportDataInProgress: Boolean = false,
    val importDataInProgress: Boolean = false,
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
    /** Count of templates with activity after the last mine watermark (`null` = not loaded yet; `0` = none pending). Shown on the variability preview button. */
    val nextMineTemplateCount: Int? = null,
)
