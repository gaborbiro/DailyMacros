package dev.gaborbiro.dailymacros.repo.records

import dev.gaborbiro.dailymacros.data.db.model.NutrientsDBModel
import dev.gaborbiro.dailymacros.data.db.model.RecordDBModel
import dev.gaborbiro.dailymacros.data.db.model.RecordWithTemplateAndNutrients
import dev.gaborbiro.dailymacros.data.db.model.TemplateDBModel
import dev.gaborbiro.dailymacros.repo.records.domain.model.Nutrients
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.records.domain.model.RecordToSave
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template
import dev.gaborbiro.dailymacros.repo.records.domain.model.TemplateToSave
import java.time.LocalDateTime

internal class DBMapper {

    // -------- Domain <— DB: Template --------

    fun map(template: TemplateDBModel): Template {
        return Template(
            id = requireNotNull(template.id),
            image = template.image,
            name = template.name,
            description = template.description,
            nutrients = null,
        )
    }

    fun map(
        template: TemplateDBModel,
        nutrients: NutrientsDBModel?,
    ): Template {
        return Template(
            id = requireNotNull(template.id),
            image = template.image,
            name = template.name,
            description = template.description,
            nutrients = nutrients?.let(::map)
        )
    }

    // -------- Domain <— DB: Nutrients --------

    private fun map(template: NutrientsDBModel): Nutrients {
        return Nutrients(
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

    // -------- Domain <— DB: RecordWithTemplateAndNutrients --------

    fun map(records: List<RecordWithTemplateAndNutrients>): List<Record> {
        return records.map(::map)
    }

    fun map(record: RecordWithTemplateAndNutrients): Record {
        return Record(
            id = record.record.id!!,
            timestamp = record.record.timestamp,
            template = map(record.template, record.nutrients),
        )
    }

    // -------- DB <— Domain: write helpers --------

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

    // Domain Nutrients -> DB Nutrients (templateId set by caller via .copy)
    fun map(nutrients: Nutrients): NutrientsDBModel {
        return NutrientsDBModel(
            templateId = 0L, // placeholder; overwrite via .copy(templateId = X)
            calories = nutrients.calories,
            protein = nutrients.protein,
            carbohydrates = nutrients.carbohydrates,
            ofWhichSugar = nutrients.ofWhichSugar,
            fat = nutrients.fat,
            ofWhichSaturated = nutrients.ofWhichSaturated,
            salt = nutrients.salt,
            fibre = nutrients.fibre
        )
    }
}
