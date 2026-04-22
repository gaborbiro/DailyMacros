package dev.gaborbiro.dailymacros.features.trends.model

import androidx.compose.ui.graphics.Color

data class TrendsUiState(
    val charts: List<TrendsChartUiModel> = emptyList(),
    val settings: TrendsSettingsUIModel = TrendsSettingsUIModel.Hidden,
    val showTargetsSettings: Boolean = false,
)

enum class Timescale { DAYS, WEEKS, MONTHS }

data class TrendsChartUiModel(val datasets: List<ChartDataset>)
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
