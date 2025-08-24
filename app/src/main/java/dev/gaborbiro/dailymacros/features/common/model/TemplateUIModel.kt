package dev.gaborbiro.dailymacros.features.common.model

internal class TemplateUIModel(
    val templateId: Long,
    val images: List<String>,
    val title: String,
    val description: String?,
) : BaseListItemUIModel(id = templateId, contentType = "template")

internal class TemplatesStartUIModel : BaseListItemUIModel(id = Long.MIN_VALUE, contentType = "")

internal class TemplatesEndUIModel : BaseListItemUIModel(id = Long.MAX_VALUE, contentType = "")
