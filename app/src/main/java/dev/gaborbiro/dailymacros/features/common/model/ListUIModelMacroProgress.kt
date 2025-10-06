package dev.gaborbiro.dailymacros.features.common.model

import android.util.Range
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

@Stable
data class ListUIModelMacroProgress(
    override val listItemId: Long,
    val dayTitle: String,
    val infoMessage: String? = null,
    val progress: List<MacroProgressItem>,
) : ListUIModelBase(listItemId = listItemId, contentType = "macroTable")

data class MacroProgressItem(
    val title: String,
    val progress0to1: Float,
    val progressLabel: String,
    val targetRange0to1: Range<Float>,
    val rangeLabel: String,
    val color: Color,
)
