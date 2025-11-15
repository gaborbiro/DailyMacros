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

/**
 * Represents a change indicator with direction and value.
 */
internal data class ChangeIndicator(
    val direction: ChangeDirection,
    val value: String, // e.g., "+5.2%", "-3.1%", "0%"
)

/**
 * Direction of change for the indicator.
 */
internal enum class ChangeDirection {
    UP,    // Rising (green/positive)
    DOWN,  // Falling (red/negative)
    NEUTRAL // No change or minimal change
}

