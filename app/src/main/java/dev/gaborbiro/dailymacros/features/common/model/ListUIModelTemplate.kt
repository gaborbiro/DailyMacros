package dev.gaborbiro.dailymacros.features.common.model

internal class ListUIModelTemplate(
    val templateId: Long,
    val images: List<String>,
    val title: String,
    val description: String?,
) : ListUIModelBase(listItemId = templateId, contentType = "template")

internal class ListUIModelTemplatesStart : ListUIModelBase(listItemId = Long.MIN_VALUE, contentType = "")

internal class ListUIModelTemplates : ListUIModelBase(listItemId = Long.MAX_VALUE, contentType = "")
