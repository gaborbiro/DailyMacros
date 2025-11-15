package dev.gaborbiro.dailymacros.features.common.model

import android.util.Range
import androidx.compose.ui.graphics.Color
import dev.gaborbiro.dailymacros.design.ExtraColorScheme

/**
 * Model for weekly summary macro items with change indicators.
 * This is separate from MacroProgressItem to allow different display logic.
 */
internal data class WeeklySummaryMacroProgressItem(
    val title: String,
    val progress0to1: Float,
    val progressLabel: String,
    val targetRange0to1: Range<Float>,
    val changeIndicator: ChangeIndicator,
    val color: (ExtraColorScheme) -> Color,
)
