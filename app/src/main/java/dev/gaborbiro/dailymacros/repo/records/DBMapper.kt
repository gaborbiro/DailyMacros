package dev.gaborbiro.dailymacros.repo.records

import dev.gaborbiro.dailymacros.data.db.records.model.RecordAndTemplateDBModel
import dev.gaborbiro.dailymacros.data.db.records.model.RecordDBModel
import dev.gaborbiro.dailymacros.data.db.records.model.TemplateDBModel
import dev.gaborbiro.dailymacros.repo.records.domain.model.Nutrients
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.records.domain.model.RecordToSave
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template
import dev.gaborbiro.dailymacros.repo.records.domain.model.TemplateToSave
import java.time.LocalDateTime

internal class DBMapper {

    fun map(template: TemplateDBModel): Template {
        return Template(
            id = template.id!!,
            image = template.image,
            name = template.name,
            description = template.description,
            nutrients = mapNutrients(template)
        )
    }

    private fun mapNutrients(template: TemplateDBModel): Nutrients? {
        return if (template.calories == null &&
            template.protein == null &&
            template.fat == null &&
            template.carbohydrates == null &&
            template.ofWhichSugar == null &&
            template.ofWhichSaturated == null &&
            template.salt == null &&
            template.fibre == null
        ) {
            null
        } else {
            Nutrients(
                calories = template.calories,
                protein = template.protein,
                fat = template.fat,
                carbohydrates = template.carbohydrates,
                ofWhichSugar = template.ofWhichSugar,
                ofWhichSaturated = template.ofWhichSaturated,
                salt = template.salt,
                fibre = template.fibre,
            )
        }
    }

    fun map(records: List<RecordAndTemplateDBModel>): List<Record> {
        return records.map(::map)
    }

    fun map(record: RecordAndTemplateDBModel): Record {
        return Record(
            id = record.record.id!!,
            timestamp = record.record.timestamp,
            template = map(record.template),
        )
    }

    fun map(record: RecordToSave, templateId: Long): RecordDBModel {
        return RecordDBModel(
            timestamp = record.timestamp,
            templateId = templateId,
        )
    }

    fun map(template: TemplateToSave): TemplateDBModel {
        return TemplateDBModel(
            image = template.image,
            name = template.name,
            description = template.description,
        )
    }

    fun map(record: Record, dateTime: LocalDateTime?/* = null */): RecordDBModel {
        return RecordDBModel(
            timestamp = dateTime ?: record.timestamp,
            templateId = record.template.id
        )
    }
}
