package dev.gaborbiro.dailymacros.data.db.model.entity

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
) : BaseEntity()
