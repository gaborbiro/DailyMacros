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
            childColumns = [COLUMN_TEMPLATE_ID],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = [COLUMN_TEMPLATE_ID]),
        Index(value = ["image"]),
        Index(value = [COLUMN_TEMPLATE_ID, "sortOrder"], unique = true)
    ]
)
data class ImageEntity(
    @ColumnInfo(name = COLUMN_TEMPLATE_ID) val templateId: Long,
    val image: String,
    val sortOrder: Int,
) : BaseEntity()
