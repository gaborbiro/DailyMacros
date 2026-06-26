package dev.gaborbiro.dailymacros.features.trends.model

import androidx.compose.ui.graphics.Color

data class TrendsUiState(
    val charts: List<TrendsChartUiModel> = emptyList(),
    val settings: TrendsSettingsUIModel = TrendsSettingsUIModel.Hidden,
    val showTargetsSettings: Boolean = false,
    val insights: Map<String, String> = emptyMap(),
    val insightsDateRange: String? = null,
    val insightsLoading: Boolean = false,
    val insightsError: String? = null,
    val ongoingInsights: Map<String, String> = emptyMap(),
    val ongoingInsightsDateRange: String? = null,
    val ongoingInsightsLoading: Boolean = false,
    val ongoingInsightsError: String? = null,
)

enum class Timescale { DAYS, WEEKS, MONTHS }

data class TrendsChartUiModel(
    val title: String,
    val datasets: List<ChartDataset>,
    val pinnedMaxY: Double? = null,
)
data class ChartDataset(
    val name: String,
    val color: Color,
    val set: List<ChartDataPoint>,
    val current: ChartDataPoint?,
    /** Daily target lower bound (same Y unit as the series), when enabled in settings. */
    val targetMinY: Double? = null,
    /** Daily target upper bound (same Y unit as the series), when enabled in settings. */
    val targetMaxY: Double? = null,
)
data class ChartDataPoint(val index: Int, val label: String, val value: Double?)

sealed class TrendsSettingsUIModel {
    data object Hidden : TrendsSettingsUIModel()
    data class Show(
        val dayQualifier: DayQualifier,
        val qualifiedDaysThreshold: Long,
    ) : TrendsSettingsUIModel()
}
