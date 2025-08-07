package dev.gaborbiro.nutri.features.overview.model

import dev.gaborbiro.nutri.data.records.domain.model.Record
import dev.gaborbiro.nutri.features.common.model.RecordViewState

data class OverviewViewState(
    val records: List<RecordViewState> = emptyList(),
    val refreshWidget: Boolean = false,
    val showUndoDeleteSnackbar: Boolean = false,
    val recordToUndelete: Record? = null,
)
