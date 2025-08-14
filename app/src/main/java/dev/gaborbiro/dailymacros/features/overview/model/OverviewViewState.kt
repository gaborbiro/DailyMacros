package dev.gaborbiro.dailymacros.features.overview.model

import dev.gaborbiro.dailymacros.data.records.domain.model.Record
import dev.gaborbiro.dailymacros.features.common.model.BaseListItem

data class OverviewViewState(
    val list: List<BaseListItem> = emptyList(),
    val showUndoDeleteSnackbar: Boolean = false,
    val recordToUndelete: Record? = null,
)
