package dev.gaborbiro.dailymacros.features.trends.model

import androidx.compose.ui.graphics.Color

internal data class TrendsViewState(
    val charts: List<TrendsChartUiModel> = emptyList(),
    val settings: TrendsSettingsUIModel = TrendsSettingsUIModel.Hidden,
)

enum class Timescale { DAYS, WEEKS, MONTHS }

data class TrendsChartUiModel(val datasets: List<ChartDataset>)
data class ChartDataset(val name: String, val color: Color, val set: List<ChartDataPoint>, val current: ChartDataPoint?)
data class ChartDataPoint(val index: Int, val label: String, val value: Double?)

sealed class TrendsSettingsUIModel {
    data object Hidden : TrendsSettingsUIModel()
    data class Show(
        val dayQualifier: DayQualifier,
        val qualifiedDaysThreshold: Long,
    ) : TrendsSettingsUIModel()
}