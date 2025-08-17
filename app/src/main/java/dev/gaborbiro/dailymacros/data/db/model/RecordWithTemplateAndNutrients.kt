package dev.gaborbiro.dailymacros.data.db.model

import androidx.room.Embedded
import androidx.room.Relation

data class RecordWithTemplateAndNutrients(
    @Embedded val record: RecordDBModel,

    @Relation(
        parentColumn = COLUMN_TEMPLATE_ID,
        entityColumn = COLUMN_ID
    )
    val template: TemplateDBModel,

    @Relation(
        parentColumn = COLUMN_TEMPLATE_ID,
        entityColumn = COLUMN_TEMPLATE_ID
    )
    val nutrients: NutrientsDBModel?, // null => no nutrient row
)
