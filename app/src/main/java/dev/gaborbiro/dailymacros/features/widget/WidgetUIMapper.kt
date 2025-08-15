package dev.gaborbiro.dailymacros.features.widget

import android.graphics.Bitmap
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template
import dev.gaborbiro.dailymacros.features.common.NutrientsUIMapper
import dev.gaborbiro.dailymacros.features.widget.model.TemplateUIModel
import dev.gaborbiro.dailymacros.data.bitmap.ImageStore

internal class WidgetUIMapper(
    private val imageStore: ImageStore,
    private val nutrientsUIMapper: NutrientsUIMapper,
) {

    fun map(templates: List<Template>, thumbnail: Boolean): List<TemplateUIModel> {
        return templates.map {
            map(it, thumbnail)
        }
    }

    private fun map(template: Template, thumbnail: Boolean): TemplateUIModel {
        val bitmap: Bitmap? = template.image?.let { imageStore.read(it, thumbnail) }
        val description = nutrientsUIMapper.map(template.nutrients, isShort = true)
        return TemplateUIModel(
            templateId = template.id,
            bitmap = bitmap,
            title = template.name,
            description = description,
        )
    }
}
