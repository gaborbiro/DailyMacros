package dev.gaborbiro.dailymacros.features.trends.model

import androidx.compose.ui.graphics.Color

internal data class TrendsViewState(
    val chartsData: List<MacroChartData> = emptyList(),
    val trendsSettings: TrendsSettingsUIModel = TrendsSettingsUIModel.Hidden,
)

enum class TimeScale { DAYS, WEEKS, MONTHS }

data class MacroChartData(val datasets: List<MacroChartDataset>)
data class MacroChartDataset(val name: String, val color: Color, val set: List<MacroChartDataPoint>, val now: MacroChartDataPoint?)
data class MacroChartDataPoint(val index: Int, val label: String, val value: Double?)

sealed class TrendsSettingsUIModel {
    data object Hidden : TrendsSettingsUIModel()
    data class Show(
        val dailyAggregationMode: DailyAggregationMode,
        val qualifiedDaysThreshold: Long,
    ) : TrendsSettingsUIModel()
}