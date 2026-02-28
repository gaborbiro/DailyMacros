package dev.gaborbiro.dailymacros.features.overview.model

import dev.gaborbiro.dailymacros.features.common.model.ListUiModelBase
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record


data class OverviewUiState(
    val items: List<ListUiModelBase> = emptyList(),
    val isLoadingMore: Boolean = false,
    val hasMoreData: Boolean = true,
    val showAddWidgetButton: Boolean = false,
    val showUndoDeleteSnackbar: Boolean = false,
    val recordToUndelete: Record? = null,
    val showCoachMark: Boolean = false,
    val showSettingsButton: Boolean = false,
    val showTrendsButton: Boolean = true,
)
