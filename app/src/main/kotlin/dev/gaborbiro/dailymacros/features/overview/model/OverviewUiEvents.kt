package dev.gaborbiro.dailymacros.features.overview.model

sealed class OverviewUiEvents {
    data class EditRecord(val recordId: Long) : OverviewUiEvents()
    data class ViewImage(val recordId: Long) : OverviewUiEvents()
    data object OpenSettingsScreen : OverviewUiEvents()
    data object OpenTrendsScreen : OverviewUiEvents()
}
