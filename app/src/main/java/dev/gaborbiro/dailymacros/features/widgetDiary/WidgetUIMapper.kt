package dev.gaborbiro.dailymacros.features.widgetDiary

import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.common.model.ListUiModelQuickPick
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template

internal class WidgetUIMapper(
    private val macrosUIMapper: MacrosUIMapper,
) {

    fun map(templates: List<Template>): List<ListUiModelQuickPick> {
        return templates.map {
            map(it)
        }
    }

    private fun map(template: Template): ListUiModelQuickPick {
        val macros = template.macros?.let(macrosUIMapper::mapMacroAmounts)
        return ListUiModelQuickPick(
            templateId = template.dbId,
            images = template.images,
            title = template.name,
            macros = macros,
        )
    }
}
