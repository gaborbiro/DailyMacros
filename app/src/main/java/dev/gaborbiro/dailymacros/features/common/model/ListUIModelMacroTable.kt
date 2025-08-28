package dev.gaborbiro.dailymacros.features.common.model

import android.util.Range
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import java.time.LocalDate

@Stable
data class ListUIModelMacroTable(
    val date: LocalDate,
    val macros: List<MacroProgressItem>,
) : ListUIModelBase(id = date.toEpochDay(), contentType = "macroTable")

data class MacroProgressItem(
    val title: String,
    val progress: Float,
    val progressLabel: String,
    val range: Range<Float>,
    val rangeLabel: String,
    val color: Color,
)
