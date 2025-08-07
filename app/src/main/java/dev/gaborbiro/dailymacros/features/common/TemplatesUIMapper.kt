package dev.gaborbiro.dailymacros.features.common

import android.graphics.Bitmap
import dev.gaborbiro.dailymacros.data.records.domain.model.Template
import dev.gaborbiro.dailymacros.features.common.model.TemplateUIModel
import dev.gaborbiro.dailymacros.store.bitmap.BitmapStore

class TemplatesUIMapper(
    private val bitmapStore: BitmapStore,
) {

    fun map(records: List<Template>, thumbnail: Boolean): List<TemplateUIModel> {
        return records.map {
            map(it, thumbnail)
        }
    }

    private fun map(template: Template, thumbnail: Boolean): TemplateUIModel {
        var bitmap: Bitmap? = null
        bitmap = template.image?.let { bitmapStore.read(it, thumbnail) }
        return TemplateUIModel(
            templateId = template.id,
            bitmap = bitmap,
            title = template.name,
        )
    }
}
