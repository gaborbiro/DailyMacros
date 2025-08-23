package dev.gaborbiro.dailymacros.features.common.model

import android.util.Range
import androidx.compose.runtime.Stable
import java.time.LocalDate

@Stable
data class MacroProgressTableUIModel(
    val date: LocalDate,
    val macros: List<MacroProgressItem>,
) : BaseListItemUIModel(date, "macroTable")

data class MacroProgressItem(
    val title: String,
    val progress: Float,
    val progressLabel: String,
    val range: Range<Float>,
    val rangeLabel: String,
)
