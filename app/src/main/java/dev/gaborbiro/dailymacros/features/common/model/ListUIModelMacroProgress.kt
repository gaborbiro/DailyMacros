package dev.gaborbiro.dailymacros.features.common.model

import androidx.compose.runtime.Stable

@Stable
internal data class ListUIModelMacroProgress(
    override val listItemId: Long,
    val dayTitle: String,
    val infoMessage: String? = null,
    val progress: List<MacroProgressItem>,
) : ListUIModelBase(listItemId = listItemId, contentType = "macroTable")

