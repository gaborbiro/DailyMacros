package dev.gaborbiro.dailymacros.data.db.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "request_status",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = [COLUMN_ID],
            childColumns = [COLUMN_TEMPLATE_ID],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(COLUMN_TEMPLATE_ID)]
)
data class RequestStatusEntity(
    @PrimaryKey val templateId: Long,
    val status: RequestStatus = RequestStatus.PENDING,
    val startedAt: Long = System.currentTimeMillis(),
    val message: String? = null,
)

enum class RequestStatus {
    PENDING,
    SUCCESS,
    FAILED,
}
