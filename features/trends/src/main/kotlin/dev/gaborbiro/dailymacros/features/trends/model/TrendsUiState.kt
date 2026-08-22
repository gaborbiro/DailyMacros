package dev.gaborbiro.dailymacros.features.trends.model

import androidx.compose.ui.graphics.Color

data class TrendsUiState(
    val charts: List<TrendsChartUiModel> = emptyList(),
    val settings: TrendsSettingsUIModel = TrendsSettingsUIModel.Hidden,
    val showTargetsSettings: Boolean = false,
    val weeklyInsights: Map<String, String> = emptyMap(),
    val weeklyInsightsDateRange: String? = null,
    val weeklyInsightsLoading: Boolean = false,
    val weeklyInsightsError: String? = null,
    val ongoingWeekInsights: String? = null,
    val ongoingWeekInsightsDateRange: String? = null,
    val ongoingWeekInsightsLoading: Boolean = false,
    val ongoingWeekInsightsError: String? = null,
    val aiInsightsEnabled: Boolean = false,
    val weeklyInsightsFetchedAtLabel: String? = null,
    val ongoingInsightsFetchedAtLabel: String? = null,
    val weeklyInsightsWeekAssessment: String? = null,
    /** One-shot: set once a pending scroll request (see TrendsViewModel.onInitialScrollRequested)
     *  resolves to an index in the currently loaded [charts] - the view scrolls all charts to it
     *  and clears it via onChartScrollHandled(). */
    val scrollToChartIndex: Int? = null,
)

enum class Timescale { DAYS, WEEKS, MONTHS }

data class TrendsChartUiModel(
    val title: String,
    val datasets: List<ChartDataset>,
    val pinnedMaxY: Double? = null,
)
data class ChartDataset(
    val name: String,
    val color: Color,
    val set: List<ChartDataPoint>,
    val current: ChartDataPoint?,
    /** Daily target lower bound (same Y unit as the series), when enabled in settings. */
    val targetMinY: Double? = null,
    /** Daily target upper bound (same Y unit as the series), when enabled in settings. */
    val targetMaxY: Double? = null,
)
/** [epochDay] is the calendar day this point represents: the day itself for a DAYS chart, the
 *  week's start day for WEEKS, the month's first day for MONTHS - see TrendsUiMapper. Lets a tap
 *  on this point resolve to a real date to jump to in Overview. */
data class ChartDataPoint(val index: Int, val label: String, val value: Double?, val epochDay: Long)

sealed class TrendsSettingsUIModel {
    data object Hidden : TrendsSettingsUIModel()
    data class Show(
        val dayQualifier: DayQualifier,
        val qualifiedDaysThreshold: Long,
    ) : TrendsSettingsUIModel()
}
