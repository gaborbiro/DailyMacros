package dev.gaborbiro.dailymacros.features.overview.model

import android.util.Range
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import dev.gaborbiro.dailymacros.design.ExtraColorScheme
import dev.gaborbiro.dailymacros.features.common.model.ListUiModelBase

@Stable
internal data class ListUiModelDailySummary(
    override val listItemId: Long,
    val dayTitle: String,
    val infoMessage: String? = null,
    val entries: List<DailySummaryEntry>,
) : ListUiModelBase(listItemId = listItemId, contentType = "daily summary")

internal data class DailySummaryEntry(
    val title: String,
    val progress0to1: Float,
    val progressLabel: String,
    val targetRange0to1: Range<Float>,
    val targetRangeLabel: String,
    val color: (ExtraColorScheme) -> Color,
)
