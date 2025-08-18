package dev.gaborbiro.dailymacros.repo.records

import dev.gaborbiro.dailymacros.data.db.model.RecordJoined
import dev.gaborbiro.dailymacros.data.db.model.TemplateJoined
import dev.gaborbiro.dailymacros.data.db.model.entity.NutrientsEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.RecordEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TemplateEntity
import dev.gaborbiro.dailymacros.repo.records.domain.model.Nutrients
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.records.domain.model.RecordToSave
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template
import dev.gaborbiro.dailymacros.repo.records.domain.model.TemplateToSave
import java.time.LocalDateTime

internal class ApiMapper {

    // -------- Domain <— DB: Template --------

    fun map(template: TemplateJoined): Template {
        val orderedImages = template.images.sortedBy { it.sortOrder }.map { it.image }
        return Template(
            dbId = requireNotNull(template.entity.id),
            images = orderedImages,
            name = template.entity.name,
            description = template.entity.description,
            nutrients = template.nutrients?.let(::map)
        )
    }

    // -------- Domain <— DB: Nutrients --------

    private fun map(template: NutrientsEntity): Nutrients {
        return Nutrients(
            calories = template.calories,
            protein = template.protein,
            fat = template.fat,
            carbohydrates = template.carbohydrates,
            ofWhichSugar = template.ofWhichSugar,
            ofWhichSaturated = template.ofWhichSaturated,
            salt = template.salt,
            fibre = template.fibre,
            notes = template.notes,
        )
    }

    // -------- Domain <— DB: RecordJoined --------

    fun map(records: List<RecordJoined>): List<Record> {
        return records.map(::map)
    }

    fun map(record: RecordJoined): Record {
        return Record(
            dbId = record.entity.id!!,
            timestamp = record.entity.timestamp,
            template = map(record.template),
        )
    }

    // -------- DB <— Domain: write helpers --------

    fun map(record: RecordToSave, templateId: Long): RecordEntity {
        return RecordEntity(
            timestamp = record.timestamp,
            templateId = templateId,
        )
    }

    fun map(template: TemplateToSave): TemplateEntity {
        return TemplateEntity(
            name = template.name,
            description = template.description,
        )
    }

    fun map(record: Record, dateTime: LocalDateTime?): RecordEntity {
        return RecordEntity(
            timestamp = dateTime ?: record.timestamp,
            templateId = record.template.dbId
        )
    }

    // Domain Nutrients -> NutrientsEntity (templateId set by caller via .copy)
    fun map(nutrients: Nutrients, id: Long?, templateId: Long): NutrientsEntity {
        return NutrientsEntity(
            templateId = templateId,
            calories = nutrients.calories,
            protein = nutrients.protein,
            carbohydrates = nutrients.carbohydrates,
            ofWhichSugar = nutrients.ofWhichSugar,
            fat = nutrients.fat,
            ofWhichSaturated = nutrients.ofWhichSaturated,
            salt = nutrients.salt,
            fibre = nutrients.fibre,
            notes = nutrients.notes,
        ).also {
            it.id = id
        }
    }
}
