package dev.gaborbiro.dailymacros.features.common.model

internal class ListUIModelQuickPick(
    val templateId: Long,
    val images: List<String>,
    val title: String,
    val macros: MacrosAmountsUIModel?,
) : ListUIModelBase(listItemId = templateId, contentType = "quick pick")

internal data object ListUIModelQuickPickHeader : ListUIModelBase(listItemId = Long.MIN_VALUE, contentType = "")

internal data object ListUIModelQuickPickFooter : ListUIModelBase(listItemId = Long.MAX_VALUE, contentType = "")
