package dev.gaborbiro.dailymacros.data.db.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "templates",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = [COLUMN_ID],
            childColumns = ["parentTemplateId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["parentTemplateId"]),
    ],
)
data class TemplateEntity(
    val name: String,
    val description: String,
    /** Immediate parent when this template was created by forking/editing from another template. */
    val parentTemplateId: Long? = null,
    /** Epoch ms when the template row was first created (migration default: 0 = epoch start). */
    @ColumnInfo(name = "createdAtEpochMs") val createdAtEpochMs: Long = 0L,
    /** Epoch ms when the template row was last updated (migration default: 0). */
    @ColumnInfo(name = "updatedAtEpochMs") val updatedAtEpochMs: Long = 0L,
) : BaseEntity()
