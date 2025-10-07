package dev.gaborbiro.dailymacros.repo.records

import dev.gaborbiro.dailymacros.data.db.model.RecordJoined
import dev.gaborbiro.dailymacros.data.db.model.TemplateJoined
import dev.gaborbiro.dailymacros.data.db.model.entity.MacrosEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.RecordEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.RequestStatus
import dev.gaborbiro.dailymacros.data.db.model.entity.TemplateEntity
import dev.gaborbiro.dailymacros.repo.records.domain.model.Macros
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template
import dev.gaborbiro.dailymacros.repo.records.domain.model.TemplateToSave
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

internal class RecordsApiMapper {

    // -------- Domain <— DB: Template --------

    fun map(template: TemplateJoined): Template {
        val orderedImages = template.images.sortedBy { it.sortOrder }.map { it.image }
        return Template(
            dbId = requireNotNull(template.entity.id) { "Template db id null" },
            images = orderedImages,
            name = template.entity.name,
            description = template.entity.description,
            macros = template.macros?.let(::map),
            isPending = template.requestStatus?.status == RequestStatus.PENDING,
        )
    }

    // -------- Domain <— DB: Macros --------

    private fun map(template: MacrosEntity): Macros {
        return Macros(
            calories = template.calories,
            protein = template.protein,
            fat = template.fat,
            ofWhichSaturated = template.ofWhichSaturated,
            carbohydrates = template.carbohydrates,
            ofWhichSugar = template.ofWhichSugar,
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
        val epochMillis = record.entity.epochMillis
        val zoneId = ZoneId.of(record.entity.zoneId)
        return Record(
            recordId = record.entity.id!!,
            timestamp = Instant.ofEpochMilli(epochMillis).atZone(zoneId),
            template = map(record.template),
        )
    }

    // -------- DB <— Domain: write helpers --------

    fun map(templateId: Long, timestamp: ZonedDateTime): RecordEntity {
        return RecordEntity(
            timestamp = timestamp.toLocalDateTime(),
            zoneId = timestamp.zone.id,
            epochMillis = timestamp.toInstant().toEpochMilli(),
            templateId = templateId,
        )
    }

    fun map(template: TemplateToSave): TemplateEntity {
        return TemplateEntity(
            name = template.name,
            description = template.description,
        )
    }

    // Domain Macros -> MacrosEntity (templateId set by caller via .copy)
    fun map(macros: Macros, id: Long?, templateId: Long): MacrosEntity {
        return MacrosEntity(
            templateId = templateId,
            calories = macros.calories,
            protein = macros.protein,
            carbohydrates = macros.carbohydrates,
            ofWhichSugar = macros.ofWhichSugar,
            fat = macros.fat,
            ofWhichSaturated = macros.ofWhichSaturated,
            salt = macros.salt,
            fibre = macros.fibre,
            notes = macros.notes,
        ).also {
            it.id = id
        }
    }
}
