package dev.gaborbiro.dailymacros.features.overview.model

import dev.gaborbiro.dailymacros.features.common.model.ListUIModelBase
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record


data class OverviewViewState(
    val items: List<ListUIModelBase> = emptyList(),
    val showAddWidgetButton: Boolean = false,
    val showUndoDeleteSnackbar: Boolean = false,
    val recordToUndelete: Record? = null,
    val showCoachMark: Boolean = false,
    val showSettingsButton: Boolean = false,
    val showDashboardButton: Boolean = true,
)
