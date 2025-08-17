package dev.gaborbiro.dailymacros.data.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import java.time.LocalDateTime

const val COLUMN_TEMPLATE_ID = "templateId"

@Entity(
    tableName = "records",
    foreignKeys = [
        ForeignKey(
            entity = TemplateDBModel::class,
            parentColumns = arrayOf(COLUMN_ID),
            childColumns = arrayOf(COLUMN_TEMPLATE_ID),
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class RecordDBModel(
    val timestamp: LocalDateTime,
    @ColumnInfo(name = COLUMN_TEMPLATE_ID) val templateId: Long,
) : DBModel()
