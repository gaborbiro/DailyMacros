package dev.gaborbiro.dailymacros.features.widgets.model

import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelBase
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel

class ListUiModelQuickPick(
    val templateId: Long,
    val images: List<String>,
    val title: String,
    val nutrients: NutrientsUiModel?,
) : ListUiModelBase(listItemId = templateId, contentType = "quick pick")

data object ListUiModelQuickPickHeader : ListUiModelBase(listItemId = Long.MIN_VALUE, contentType = "")

data object ListUiModelQuickPickFooter : ListUiModelBase(listItemId = Long.MAX_VALUE, contentType = "")
