package dev.gaborbiro.dailymacros.features.trends.model

import androidx.compose.ui.graphics.Color

internal data class TrendsViewState(
    val scale: TimeScale = TimeScale.DAYS,
    val charts: List<MacroChartData> = emptyList(),
)

enum class TimeScale { DAYS, WEEKS, MONTHS }

data class MacroChartData(val datasets: List<MacroChartDataset>)
data class MacroChartDataset(val name: String, val color: Color, val set: List<MacroChartDataPoint>, val now: MacroChartDataPoint?)
data class MacroChartDataPoint(val index: Int, val label: String, val value: Double?)
