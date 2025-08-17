package dev.gaborbiro.dailymacros.data.db.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "templates",
    indices = [
        Index(
            value = ["name", "image"],
            unique = true,
        )
    ],
)
data class TemplateDBModel(
    val image: String?,
    val name: String,
    val description: String,
) : DBModel()
