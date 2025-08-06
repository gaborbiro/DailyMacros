package dev.gaborbiro.nutri.store.db.records.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import dev.gaborbiro.nutri.store.db.common.DBModel

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
    val calories: Int? = null,
    val protein: Float? = null,
    val carbohydrates: Float? = null,
    val fat: Float? = null,
) : DBModel() {

    @Ignore
    constructor(
        id: Long,
        image: String?,
        name: String,
        description: String,
        calories: Int? = null,
        protein: Float? = null,
        carbohydrates: Float? = null,
        fat: Float? = null,
    ) : this(
        image,
        name,
        description,
        calories,
        protein,
        carbohydrates,
        fat,
    ) {
        this.id = id
    }
}
