package dev.gaborbiro.dailymacros.features.common.model

import android.util.Range
import androidx.compose.ui.graphics.Color

internal data class MacroProgressItem(
    val title: String,
    val progress0to1: Float,
    val progressLabel: String,
    val targetRange0to1: Range<Float>,
    val targetRangeLabel: String,
    val color: Color,
)