package dev.gaborbiro.dailymacros.features.overview.model

import dev.gaborbiro.dailymacros.data.records.domain.model.Record
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel

data class OverviewViewState(
    val records: List<RecordUIModel> = emptyList(),
    val refreshWidget: Boolean = false,
    val showUndoDeleteSnackbar: Boolean = false,
    val recordToUndelete: Record? = null,
    val todaysNutrientProgress: NutrientProgress? = null,
    val yesterdaysNutrientProgress: NutrientProgress? = null,
)
