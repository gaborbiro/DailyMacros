package dev.gaborbiro.dailymacros.features.widgets

import dev.gaborbiro.dailymacros.features.shared.TemplateUiMapper
import dev.gaborbiro.dailymacros.features.widgets.model.ListUiModelQuickPick
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import javax.inject.Inject

class WidgetUiMapper @Inject constructor(
    private val templateUiMapper: TemplateUiMapper,
) {

    fun map(templates: List<Template>): List<ListUiModelQuickPick> {
        return templates.map {
            map(it)
        }
    }

    private fun map(template: Template): ListUiModelQuickPick {
        val nutrients = templateUiMapper.mapRecordNutrients(template.nutrients)
        return ListUiModelQuickPick(
            templateId = template.dbId,
            imageFilename = templateUiMapper.getBestPhoto(template),
            title = template.name,
            nutrients = nutrients,
        )
    }
}
