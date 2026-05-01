package dev.gaborbiro.dailymacros.repositories.records

import dev.gaborbiro.dailymacros.data.db.model.RecordJoined
import dev.gaborbiro.dailymacros.data.db.model.TemplateJoined
import dev.gaborbiro.dailymacros.data.db.model.entity.MacrosEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.QuickPickOverrideEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.RecordEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.RequestStatusEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TemplateEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TopContributorsEntity
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class RecordsApiMapper {

    // -------- Domain <— DB: Template --------

    fun map(template: TemplateJoined): Template {
        val ordered = template.images.sortedBy { it.sortOrder }
        val orderedImages = ordered.map { it.image }
        val representativeFlags = ordered.map { it.isRepresentativeMealPhoto }
        return Template(
            dbId = requireNotNull(template.entity.id) { "Template db id null" },
            images = orderedImages,
            isRepresentativeOfMealByImageIndex = representativeFlags,
            name = template.entity.name,
            description = template.entity.description,
            parentTemplateId = template.entity.parentTemplateId,
            nutrients = template.macros?.let(::map) ?: TemplateNutrientBreakdown(),
            notes = template.macros?.notes ?: "",
            mealComponents = decodeMealComponentsJson(template.macros?.analysisComponentsJson),
            topContributors = template.topContributors?.let(::map) ?: TopContributors(),
            isPending = template.requestStatus?.status == RequestStatusEntity.Status.PENDING,
            quickPickOverride = map(template.quickPickOverride),
        )
    }

    private fun map(quickPickOverride: QuickPickOverrideEntity?): Template.QuickPickOverride? {
        return when (quickPickOverride?.overrideType) {
            QuickPickOverrideEntity.OverrideType.INCLUDE -> Template.QuickPickOverride.INCLUDE
            QuickPickOverrideEntity.OverrideType.EXCLUDE -> Template.QuickPickOverride.EXCLUDE
            null -> null
        }
    }

    // -------- Domain <— DB: Macros --------

    fun mapToTemplateNutrients(macros: MacrosEntity): TemplateNutrientBreakdown = map(macros)

    private fun map(template: MacrosEntity): TemplateNutrientBreakdown {
        return TemplateNutrientBreakdown(
            calories = template.calories,
            protein = template.protein,
            fat = template.fat,
            ofWhichSaturated = template.ofWhichSaturated,
            carbs = template.carbohydrates,
            ofWhichSugar = template.ofWhichSugar,
            ofWhichAddedSugar = template.ofWhichAddedSugar,
            salt = template.salt,
            fibre = template.fibre,
        )
    }

    private fun map(template: TopContributorsEntity): TopContributors {
        return TopContributors(
            topProteinContributors = template.topProteinContributors,
            topFatContributors = template.topFatContributors,
            topSaturatedFatContributors = template.topSaturatedFatContributors,
            topCarbsContributors = template.topCarbohydratesContributors,
            topSugarContributors = template.topSugarContributors,
            topAddedSugarContributors = template.topAddedSugarContributors,
            topSaltContributors = template.topSaltContributors,
            topFibreContributors = template.topFibreContributors,
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
            parentTemplateId = template.parentTemplateId,
        )
    }

    // Domain Macros -> MacrosEntity (templateId set by caller via .copy)
    fun map(
        nutrientBreakdown: TemplateNutrientBreakdown,
        notes: String?,
        analysisComponentsJson: String?,
        id: Long?,
        templateId: Long
    ): MacrosEntity {
        return MacrosEntity(
            templateId = templateId,
            calories = nutrientBreakdown.calories,
            protein = nutrientBreakdown.protein,
            carbohydrates = nutrientBreakdown.carbs,
            ofWhichSugar = nutrientBreakdown.ofWhichSugar,
            ofWhichAddedSugar = nutrientBreakdown.ofWhichAddedSugar,
            fat = nutrientBreakdown.fat,
            ofWhichSaturated = nutrientBreakdown.ofWhichSaturated,
            salt = nutrientBreakdown.salt,
            fibre = nutrientBreakdown.fibre,
            notes = notes,
            analysisComponentsJson = analysisComponentsJson,
        ).also {
            it.id = id
        }
    }

    // Domain TopContributors -> TopContributorsEntity (templateId set by caller via .copy)
    fun map(
        topContributors: TopContributors,
        id: Long?,
        templateId: Long
    ): TopContributorsEntity {
        return TopContributorsEntity(
            templateId = templateId,
            topProteinContributors = topContributors.topProteinContributors,
            topCarbohydratesContributors = topContributors.topCarbsContributors,
            topSugarContributors = topContributors.topSugarContributors,
            topAddedSugarContributors = topContributors.topAddedSugarContributors,
            topFatContributors = topContributors.topFatContributors,
            topSaturatedFatContributors = topContributors.topSaturatedFatContributors,
            topSaltContributors = topContributors.topSaltContributors,
            topFibreContributors = topContributors.topFibreContributors,
        ).also {
            it.id = id
        }
    }
}
