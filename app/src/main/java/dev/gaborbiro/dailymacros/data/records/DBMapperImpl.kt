package dev.gaborbiro.dailymacros.data.records

import dev.gaborbiro.dailymacros.data.records.domain.model.Nutrients
import dev.gaborbiro.dailymacros.data.records.domain.model.Record
import dev.gaborbiro.dailymacros.data.records.domain.model.Template
import dev.gaborbiro.dailymacros.data.records.domain.model.RecordToSave
import dev.gaborbiro.dailymacros.data.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.store.db.records.model.RecordAndTemplateDBModel
import dev.gaborbiro.dailymacros.store.db.records.model.RecordDBModel
import dev.gaborbiro.dailymacros.store.db.records.model.TemplateDBModel
import java.time.LocalDateTime

class DBMapperImpl : DBMapper {

    override fun map(template: TemplateDBModel): Template {
        return Template(
            id = template.id!!,
            image = template.image,
            name = template.name,
            description = template.description,
            nutrients = Nutrients(
                calories = template.calories,
                protein = template.protein,
                fat = template.fat,
                carbohydrates = template.carbohydrates,
                ofWhichSugar = template.ofWhichSugar,
                ofWhichSaturated = template.ofWhichSaturated,
                salt = template.salt,
            )
        )
    }

    override fun map(records: List<RecordAndTemplateDBModel>): List<Record> {
        return records.map(::map)
    }

    override fun map(record: RecordAndTemplateDBModel): Record {
        return Record(
            id = record.record.id!!,
            timestamp = record.record.timestamp,
            template = map(record.template),
        )
    }

    override fun map(record: RecordToSave, templateId: Long): RecordDBModel {
        return RecordDBModel(
            timestamp = record.timestamp,
            templateId = templateId,
        )
    }

    override fun map(template: TemplateToSave): TemplateDBModel {
        return TemplateDBModel(
            image = template.image,
            name = template.name,
            description = template.description,
        )
    }

    override fun map(record: Record, dateTime: LocalDateTime?/* = null */): RecordDBModel {
        return RecordDBModel(
            timestamp = dateTime ?: record.timestamp,
            templateId = record.template.id
        )
    }
}
