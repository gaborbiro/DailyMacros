package dev.gaborbiro.nutri.store.db.records.model

import androidx.room.Embedded
import androidx.room.Relation
import dev.gaborbiro.nutri.store.db.common.COLUMN_ID

class RecordAndTemplateDBModel(
    @Embedded val record: RecordDBModel,
    @Relation(
        parentColumn = COLUMN_TEMPLATE,
        entityColumn = COLUMN_ID
    ) val template: TemplateDBModel,
)
