package dev.gaborbiro.dailymacros.store.db.records.model

import androidx.room.Embedded
import androidx.room.Relation
import dev.gaborbiro.dailymacros.store.db.common.COLUMN_ID

class RecordAndTemplateDBModel(
    @Embedded val record: RecordDBModel,
    @Relation(
        parentColumn = COLUMN_TEMPLATE,
        entityColumn = COLUMN_ID
    ) val template: TemplateDBModel,
)
