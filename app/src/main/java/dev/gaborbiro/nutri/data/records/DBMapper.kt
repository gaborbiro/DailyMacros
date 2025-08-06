package dev.gaborbiro.nutri.data.records

import dev.gaborbiro.nutri.data.records.domain.model.Record
import dev.gaborbiro.nutri.data.records.domain.model.Template
import dev.gaborbiro.nutri.data.records.domain.model.RecordToSave
import dev.gaborbiro.nutri.data.records.domain.model.TemplateToSave
import dev.gaborbiro.nutri.store.db.records.model.RecordAndTemplateDBModel
import dev.gaborbiro.nutri.store.db.records.model.RecordDBModel
import dev.gaborbiro.nutri.store.db.records.model.TemplateDBModel
import java.time.LocalDateTime

interface DBMapper {

    companion object {
        fun get(): DBMapper = DBMapperImpl()
    }

    fun map(template: TemplateDBModel): Template

    fun map(records: List<RecordAndTemplateDBModel>): List<Record>

    fun map(record: RecordAndTemplateDBModel): Record

    fun map(record: RecordToSave, templateId: Long): RecordDBModel

    fun map(template: TemplateToSave): TemplateDBModel

    fun map(record: Record, dateTime: LocalDateTime? = null): RecordDBModel
}
