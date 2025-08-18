package dev.gaborbiro.dailymacros.features.overview.model

import dev.gaborbiro.dailymacros.features.common.model.BaseListItemUIModel
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record

data class OverviewViewState(
    val list: List<BaseListItemUIModel> = emptyList(),
    val showUndoDeleteSnackbar: Boolean = false,
    val recordToUndelete: Record? = null,
)
