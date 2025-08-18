package dev.gaborbiro.dailymacros.data.db.model

import androidx.room.Embedded
import androidx.room.Relation
import dev.gaborbiro.dailymacros.data.db.model.entity.COLUMN_ID
import dev.gaborbiro.dailymacros.data.db.model.entity.COLUMN_TEMPLATE_ID
import dev.gaborbiro.dailymacros.data.db.model.entity.RecordEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TemplateEntity

data class RecordJoined(
    @Embedded val entity: RecordEntity,

    @Relation(
        entity = TemplateEntity::class,
        parentColumn = COLUMN_TEMPLATE_ID,   // record.templateId
        entityColumn = COLUMN_ID             // templates._id
    )
    val template: TemplateJoined,
)
