package dev.gaborbiro.dailymacros.features.overview.model

sealed class OverviewUiUpdates {
    data class EditRecord(val recordId: Long) : OverviewUiUpdates()
    data class ViewImage(val recordId: Long) : OverviewUiUpdates()
    data class AddFromTemplate(val templateId: Long) : OverviewUiUpdates()
    data object OpenSettingsScreen : OverviewUiUpdates()
    data object OpenTrendsScreen : OverviewUiUpdates()
}
