package dev.gaborbiro.dailymacros.features.widget

import dev.gaborbiro.dailymacros.features.common.NutrientsUiMapper
import dev.gaborbiro.dailymacros.features.common.model.ListUiModelQuickPick
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template

internal class WidgetUiMapper(
    private val nutrientsUiMapper: NutrientsUiMapper,
) {

    fun map(templates: List<Template>): List<ListUiModelQuickPick> {
        return templates.map {
            map(it)
        }
    }

    private fun map(template: Template): ListUiModelQuickPick {
        val nutrients = nutrientsUiMapper.map(template.nutrients)
        return ListUiModelQuickPick(
            templateId = template.dbId,
            images = template.images,
            title = template.name,
            nutrients = nutrients,
        )
    }
}
