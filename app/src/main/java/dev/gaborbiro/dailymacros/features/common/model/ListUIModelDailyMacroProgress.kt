package dev.gaborbiro.dailymacros.features.common.model

import androidx.compose.runtime.Stable

@Stable
internal data class ListUIModelDailyMacroProgress(
    override val listItemId: Long,
    val dayTitle: String,
    val infoMessage: String? = null,
    val progress: List<DailyMacroProgressItem>,
) : ListUIModelBase(listItemId = listItemId, contentType = "macroTable")

