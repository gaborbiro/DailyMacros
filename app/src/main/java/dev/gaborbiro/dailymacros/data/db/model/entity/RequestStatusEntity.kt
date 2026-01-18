package dev.gaborbiro.dailymacros.data.db.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(
    tableName = "request_status",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = [COLUMN_ID],
            childColumns = [RequestStatusEntity.COLUMN_TEMPLATE_ID],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(RequestStatusEntity.COLUMN_TEMPLATE_ID)]
)
data class RequestStatusEntity(
    @PrimaryKey @ColumnInfo(name = COLUMN_TEMPLATE_ID) val templateId: Long,
    val status: Status = Status.PENDING,
    val startedAt: Long = System.currentTimeMillis(),
    val message: String? = null,
) {
    companion object {
        const val COLUMN_TEMPLATE_ID = "templateId"
    }

    enum class Status {
        PENDING,
        SUCCESS,
        FAILED,
    }
}
