package dev.gaborbiro.dailymacros.features.overview.model

import android.util.Range
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import dev.gaborbiro.dailymacros.design.ExtraColorScheme
import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelBase
import java.time.LocalDate

@Stable
internal data class ListUiModelDailySummary(
    override val listItemId: Long,
    /** The calendar day this card summarizes, so a tap on a Trends chart point can be
     *  resolved to the closest matching day card - see OverviewViewModel.onScrollToDateRequested. */
    val day: LocalDate,
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
