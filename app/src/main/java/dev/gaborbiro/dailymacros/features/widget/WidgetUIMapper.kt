package dev.gaborbiro.dailymacros.features.widget

import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.widget.model.TemplateUIModel
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template

internal class WidgetUIMapper(
    private val macrosUIMapper: MacrosUIMapper,
) {

    fun map(templates: List<Template>): List<TemplateUIModel> {
        return templates.map {
            map(it)
        }
    }

    private fun map(template: Template): TemplateUIModel {
        val description = macrosUIMapper.mapAllMacrosLabel(template.macros, isShort = true)
        return TemplateUIModel(
            templateId = template.dbId,
            images = template.images,
            title = template.name,
            description = description,
        )
    }
}
