package dev.gaborbiro.dailymacros.data.db.model.entity

import androidx.room.Entity

@Entity(
    tableName = "templates",
)
data class TemplateEntity(
    val name: String,
    val description: String,
) : BaseEntity()
