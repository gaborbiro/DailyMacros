package dev.gaborbiro.dailymacros.features.widget.model

import dev.gaborbiro.dailymacros.features.common.model.ListUiModelBase
import dev.gaborbiro.dailymacros.features.common.model.NutrientsUiModel

internal class ListUiModelQuickPick(
    val templateId: Long,
    val images: List<String>,
    val title: String,
    val nutrients: NutrientsUiModel?,
) : ListUiModelBase(listItemId = templateId, contentType = "quick pick")

internal data object ListUiModelQuickPickHeader : ListUiModelBase(listItemId = Long.MIN_VALUE, contentType = "")

internal data object ListUiModelQuickPickFooter : ListUiModelBase(listItemId = Long.MAX_VALUE, contentType = "")
