package dev.gaborbiro.dailymacros.store.db.records.model

import androidx.room.Entity
import androidx.room.ForeignKey
import dev.gaborbiro.dailymacros.store.db.common.COLUMN_ID
import dev.gaborbiro.dailymacros.store.db.common.DBModel
import java.time.LocalDateTime

const val COLUMN_TEMPLATE = "templateId"

@Entity(
    tableName = "records",
    foreignKeys = [
        ForeignKey(
            entity = TemplateDBModel::class,
            parentColumns = arrayOf(COLUMN_ID),
            childColumns = arrayOf(COLUMN_TEMPLATE),
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class RecordDBModel(
    val timestamp: LocalDateTime,
    val templateId: Long,
) : DBModel()
