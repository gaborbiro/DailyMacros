package dev.gaborbiro.dailymacros.features.widget

import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelTemplate
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template

internal class WidgetUIMapper(
    private val macrosUIMapper: MacrosUIMapper,
) {

    fun map(templates: List<Template>): List<ListUIModelTemplate> {
        return templates.map {
            map(it)
        }
    }

    private fun map(template: Template): ListUIModelTemplate {
        val description = macrosUIMapper.mapMacrosString(template.macros, isShort = true)
        return ListUIModelTemplate(
            templateId = template.dbId,
            images = template.images,
            title = template.name,
            description = description,
        )
    }
}
