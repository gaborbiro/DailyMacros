package dev.gaborbiro.dailymacros.features.common.model

internal class ListUIModelTemplate(
    val templateId: Long,
    val images: List<String>,
    val title: String,
    val description: String?,
) : ListUIModelBase(id = templateId, contentType = "template")

internal class ListUIModelTemplatesStart : ListUIModelBase(id = Long.MIN_VALUE, contentType = "")

internal class ListUIModelTemplates : ListUIModelBase(id = Long.MAX_VALUE, contentType = "")
