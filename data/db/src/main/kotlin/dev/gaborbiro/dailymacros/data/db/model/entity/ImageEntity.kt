package dev.gaborbiro.dailymacros.data.db.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "template_images",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = [COLUMN_ID],
            childColumns = [ImageEntity.COLUMN_TEMPLATE_ID],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = [ImageEntity.COLUMN_TEMPLATE_ID]),
        Index(value = ["image"]),
        Index(value = [ImageEntity.COLUMN_TEMPLATE_ID, "sortOrder"], unique = true)
    ]
)
data class ImageEntity(
    @ColumnInfo(name = COLUMN_TEMPLATE_ID) val templateId: Long,
    val image: String,
    val sortOrder: Int,
    /**
     * From nutrient analysis when available: whether this photo represents the meal (vs label-only, etc.).
     * Null means never classified.
     */
    val isRepresentativeMealPhoto: Boolean? = null,
    /**
     * MediaStore id of the gallery photo this image was created from, so auto photo recognition
     * can tell it has already been logged. Null when the image didn't come from the gallery
     * (in-app camera, share from another app) or predates this column.
     */
    val sourceMediaStoreId: Long? = null,
) : BaseEntity() {
    companion object {
        const val COLUMN_TEMPLATE_ID = "templateId"
    }
}
