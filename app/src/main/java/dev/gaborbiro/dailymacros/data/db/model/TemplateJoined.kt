package dev.gaborbiro.dailymacros.data.db.model

import androidx.room.Embedded
import androidx.room.Relation
import dev.gaborbiro.dailymacros.data.db.model.entity.COLUMN_ID
import dev.gaborbiro.dailymacros.data.db.model.entity.COLUMN_TEMPLATE_ID
import dev.gaborbiro.dailymacros.data.db.model.entity.ImageEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.NutrientsEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TemplateEntity

data class TemplateJoined(
    @Embedded val entity: TemplateEntity,

    @Relation(
        parentColumn = COLUMN_ID,
        entityColumn = COLUMN_TEMPLATE_ID
    )
    val nutrients: NutrientsEntity?, // null = no nutrient row

    @Relation(
        parentColumn = COLUMN_ID,
        entityColumn = COLUMN_TEMPLATE_ID,
        entity = ImageEntity::class
    )
    val images: List<ImageEntity>
)
