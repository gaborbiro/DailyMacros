package dev.gaborbiro.dailymacros.data.db.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "top_contributors",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = [COLUMN_ID],
            childColumns = [TopContributorsEntity.COLUMN_TEMPLATE_ID],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = [TopContributorsEntity.COLUMN_TEMPLATE_ID], unique = true)] // enforce 1:1
)
data class TopContributorsEntity(
    @ColumnInfo(name = COLUMN_TEMPLATE_ID) val templateId: Long,
    val topProteinContributors: String?,
    val topCarbohydratesContributors: String?,
    val topSugarContributors: String?,
    val topAddedSugarContributors: String?,
    val topFatContributors: String?,
    val topSaturatedFatContributors: String?,
    val topSaltContributors: String?,
    val topFibreContributors: String?,
) : BaseEntity() {
    companion object {
        const val COLUMN_TEMPLATE_ID = "templateId"
    }
}
