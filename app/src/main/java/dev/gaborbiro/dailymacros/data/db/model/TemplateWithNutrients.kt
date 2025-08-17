package dev.gaborbiro.dailymacros.data.db.model

import androidx.room.Embedded
import androidx.room.Relation

data class TemplateWithNutrients(
    @Embedded val template: TemplateDBModel,
    @Relation(
        parentColumn = COLUMN_ID,
        entityColumn = COLUMN_TEMPLATE_ID
    )
    val nutrients: NutrientsDBModel?, // null = no nutrient row
)
