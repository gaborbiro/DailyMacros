package dev.gaborbiro.dailymacros.features.trends.model

sealed class TrendsUiUpdates {
    data object NavigateBack : TrendsUiUpdates()
    data class NavigateToOverviewDate(val epochDay: Long) : TrendsUiUpdates()
}
