package dev.gaborbiro.dailymacros.features.common.model

internal class ListUiModelQuickPick(
    val templateId: Long,
    val images: List<String>,
    val title: String,
    val macros: MacrosAmountsUIModel?,
) : ListUiModelBase(listItemId = templateId, contentType = "quick pick")

internal data object ListUiModelQuickPickHeader : ListUiModelBase(listItemId = Long.MIN_VALUE, contentType = "")

internal data object ListUiModelQuickPickFooter : ListUiModelBase(listItemId = Long.MAX_VALUE, contentType = "")
