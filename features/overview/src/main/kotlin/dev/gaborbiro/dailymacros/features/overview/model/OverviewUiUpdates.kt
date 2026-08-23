package dev.gaborbiro.dailymacros.features.overview.model

sealed class OverviewUiUpdates {
    data class EditRecord(val recordId: Long) : OverviewUiUpdates()
    data class ViewImage(val recordId: Long) : OverviewUiUpdates()
    data object OpenSettingsScreen : OverviewUiUpdates()

    /**
     * [scrollToEpochDay]/[timescale] are set when opening from a daily/weekly summary tap, so
     * Trends can open pre-scrolled to that date in the matching timescale - null/null for the
     * plain "View trends" entry point. [timescale] must be one of Trends' own Timescale enum
     * names ("DAYS"/"WEEKS") - see OverviewScreen.kt for why this crosses the module boundary
     * as a plain string instead of a shared enum type.
     */
    data class OpenTrendsScreen(
        val scrollToEpochDay: Long? = null,
        val timescale: String? = null,
    ) : OverviewUiUpdates()

    data object OpenPaywallScreen : OverviewUiUpdates()
}
