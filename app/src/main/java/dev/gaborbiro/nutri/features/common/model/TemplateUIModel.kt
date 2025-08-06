package dev.gaborbiro.nutri.features.common.model

import android.graphics.Bitmap

class TemplateUIModel(
    val templateId: Long,
    val bitmap: Bitmap?,
    val title: String,
) {
    override fun toString(): String {
        return "TemplateUIModel(templateId=$templateId, bitmap=${bitmap?.byteCount ?: 0} bytes, title='$title')"
    }
}
