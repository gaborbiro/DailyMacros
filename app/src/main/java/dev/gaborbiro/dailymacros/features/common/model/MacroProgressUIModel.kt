package dev.gaborbiro.dailymacros.features.common.model

import android.util.Range
import java.time.LocalDate

data class MacroProgressUIModel(
    val date: LocalDate,
    val macros: List<MacroProgressItem>,
) : BaseListItemUIModel {
    override val id = date
}

data class MacroProgressItem(
    val title: String,
    val progress: Float,
    val progressLabel: String,
    val range: Range<Float>,
    val rangeLabel: String,
)
