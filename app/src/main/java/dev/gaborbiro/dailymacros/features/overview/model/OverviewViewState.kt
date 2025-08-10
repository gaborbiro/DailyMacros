package dev.gaborbiro.dailymacros.features.overview.model

import dev.gaborbiro.dailymacros.data.records.domain.model.Record
import dev.gaborbiro.dailymacros.features.common.model.RecordViewState
import dev.gaborbiro.dailymacros.features.common.views.MacroGoalsUIModel

data class OverviewViewState(
    val records: List<RecordViewState> = emptyList(),
    val refreshWidget: Boolean = false,
    val showUndoDeleteSnackbar: Boolean = false,
    val recordToUndelete: Record? = null,
    val macroGoals: MacroGoalsUIModel? = null,
)
