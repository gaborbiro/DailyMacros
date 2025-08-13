package dev.gaborbiro.dailymacros.store.db.records.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import dev.gaborbiro.dailymacros.store.db.common.DBModel

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
    val ofWhichSugar: Float? = null,
    val fat: Float? = null,
    val ofWhichSaturated: Float? = null,
    val salt: Float? = null,
    val fibre: Float? = null,
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
        ofWhichSugar: Float? = null,
        fat: Float? = null,
        ofWhichSaturated: Float? = null,
        salt: Float? = null,
        fibre: Float? = null,
    ) : this(
        image = image,
        name = name,
        description = description,
        calories = calories,
        protein = protein,
        carbohydrates = carbohydrates,
        ofWhichSugar = ofWhichSugar,
        fat = fat,
        ofWhichSaturated = ofWhichSaturated,
        salt = salt,
        fibre = fibre,
    ) {
        this.id = id
    }
}
