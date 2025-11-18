package dev.gaborbiro.dailymacros.features.dashboard.model

import androidx.compose.ui.graphics.Color

internal data class MacroDashboardViewState(
    val scale: TimeScale = TimeScale.DAYS,
    val datasets: List<MacroDataset> = emptyList(),
)

enum class TimeScale { DAYS, WEEKS, MONTHS }

data class MacroDataPoint(val label: String, val value: Float)
data class MacroDataset(val name: String, val color: Color, val data: List<MacroDataPoint>)