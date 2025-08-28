package dev.gaborbiro.dailymacros.features.common.model

import android.util.Range
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

@Stable
data class ListUIModelMacroProgress(
    override val listItemId: Long,
    val dayTitle: String,
    val macros: List<MacroProgressItem>,
) : ListUIModelBase(listItemId = listItemId, contentType = "macroTable")

data class MacroProgressItem(
    val title: String,
    val progress: Float,
    val progressLabel: String,
    val range: Range<Float>,
    val rangeLabel: String,
    val color: Color,
)
