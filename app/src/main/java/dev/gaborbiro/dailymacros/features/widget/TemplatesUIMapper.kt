package dev.gaborbiro.dailymacros.features.widget

import android.graphics.Bitmap
import dev.gaborbiro.dailymacros.data.records.domain.model.Template
import dev.gaborbiro.dailymacros.features.widget.model.TemplateUIModel
import dev.gaborbiro.dailymacros.store.bitmap.BitmapStore

internal class TemplatesUIMapper(
    private val bitmapStore: BitmapStore,
) {

    fun map(records: List<Template>, thumbnail: Boolean): List<TemplateUIModel> {
        return records.map {
            map(it, thumbnail)
        }
    }

    private fun map(template: Template, thumbnail: Boolean): TemplateUIModel {
        val bitmap: Bitmap? = template.image?.let { bitmapStore.read(it, thumbnail) }
        return TemplateUIModel(
            templateId = template.id,
            bitmap = bitmap,
            title = template.name,
        )
    }
}
