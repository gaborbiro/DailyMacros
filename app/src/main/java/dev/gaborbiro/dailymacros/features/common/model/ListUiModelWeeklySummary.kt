package dev.gaborbiro.dailymacros.features.common.model

import android.util.Range
import androidx.compose.ui.graphics.Color
import dev.gaborbiro.dailymacros.design.ExtraColorScheme

internal data class ListUiModelWeeklySummary(
    override val listItemId: Long,
    val entries: List<NutrientSummaryStatEntry>,
    val averageAdherence100Percentage: Int,
    val adherenceChange: ChangeIndicator?,
) : ListUiModelBase(listItemId = listItemId, contentType = "weekly summary")

/**
 * Represents a single nutrient statistic within the weekly summary.
 */
internal data class NutrientSummaryStatEntry(
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
    val value: String, // e.g., "+5%", "-3%", "0%"
)

/**
 * Direction of change for the indicator.
 */
internal enum class ChangeDirection {
    UP,    // Rising (green/positive)
    DOWN,  // Falling (red/negative)
    NEUTRAL // No change or minimal change
}
