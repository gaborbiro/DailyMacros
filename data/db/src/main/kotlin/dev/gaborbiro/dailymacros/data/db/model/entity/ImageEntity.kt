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
    /** From food recognition: true when the photo shows the dish or food in it (not e.g. label-only). */
    val coverPhoto: Boolean = false,
) : BaseEntity() {
    companion object {
        const val COLUMN_TEMPLATE_ID = "templateId"
    }
}
