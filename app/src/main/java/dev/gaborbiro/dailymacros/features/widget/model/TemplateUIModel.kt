package dev.gaborbiro.dailymacros.features.widget.model

import dev.gaborbiro.dailymacros.features.common.model.BaseListItemUIModel

internal class TemplateUIModel(
    val templateId: Long,
    val images: List<String>,
    val title: String,
    val description: String?,
): BaseListItemUIModel(id = templateId, contentType = "template")
