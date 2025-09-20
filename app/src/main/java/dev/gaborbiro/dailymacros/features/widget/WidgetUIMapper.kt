package dev.gaborbiro.dailymacros.features.widget

import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelQuickPick
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template

internal class WidgetUIMapper(
    private val macrosUIMapper: MacrosUIMapper,
) {

    fun map(templates: List<Template>): List<ListUIModelQuickPick> {
        return templates.map {
            map(it)
        }
    }

    private fun map(template: Template): ListUIModelQuickPick {
        val description = macrosUIMapper.mapMacrosString(template.macros, isShort = true)
        return ListUIModelQuickPick(
            templateId = template.dbId,
            images = template.images,
            title = template.name,
            description = description,
        )
    }
}
