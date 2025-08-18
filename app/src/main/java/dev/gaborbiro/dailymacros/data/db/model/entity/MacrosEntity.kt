package dev.gaborbiro.dailymacros.data.db.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "macros",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = [COLUMN_ID],
            childColumns = [COLUMN_TEMPLATE_ID],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = [COLUMN_TEMPLATE_ID], unique = true)] // enforce 1:1
)
data class MacrosEntity(
    @ColumnInfo(name = COLUMN_TEMPLATE_ID) val templateId: Long,
    val calories: Int?,
    val protein: Float?,
    val carbohydrates: Float?,
    val ofWhichSugar: Float?,
    val fat: Float?,
    val ofWhichSaturated: Float?,
    val salt: Float?,
    val fibre: Float?,
    val notes: String?,
) : BaseEntity()
