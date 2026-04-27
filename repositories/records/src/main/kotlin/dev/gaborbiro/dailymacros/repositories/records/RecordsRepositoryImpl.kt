package dev.gaborbiro.dailymacros.repositories.records

import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.db.RecordsDAO
import dev.gaborbiro.dailymacros.data.db.TemplatesDAO
import dev.gaborbiro.dailymacros.data.db.model.RecordJoined
import dev.gaborbiro.dailymacros.data.db.model.entity.ImageEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.MacrosEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.QuickPickOverrideEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.RecordEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TemplateEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TopContributorsEntity
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.ZonedDateTime

class RecordsRepositoryImpl(
    private val templatesDAO: TemplatesDAO,
    private val recordsDAO: RecordsDAO,
    private val mapper: RecordsApiMapper,
    private val imageStore: ImageStore,
    private val analyticsLogger: AnalyticsLogger,
) : RecordsRepository {

    // -------- Reads --------

    override suspend fun getRecords(since: ZonedDateTime?): List<Record> = recordsDAO
        .get(since?.toInstant()?.toEpochMilli() ?: 0L)
        .map(mapper::map)

    override suspend fun getRecentRecords(limit: Int): List<Record> =
        recordsDAO.getRecent(limit).map(mapper::map)

    override fun getMostRecentRecord(): Record? {
        return recordsDAO.getMostRecentRecord()
            ?.let(mapper::map)
    }

    override suspend fun getQuickPicks(count: Int): List<Template> = templatesDAO
        .getQuickPicks(count)
        .map(mapper::map)

    override suspend fun getRecordsByTemplate(templateId: Long): List<Record> = recordsDAO
        .getByTemplate(templateId)
        .let(mapper::map)

    override suspend fun countRecordsForTemplate(templateId: Long): Int =
        recordsDAO.countByTemplate(templateId)

    override fun observeRecords(
        searchTerm: String?, /* = null */
        sinceEpochMillis: Long, /* = 0L */
    ): Flow<List<Record>> {
        return try {
            val raw: Flow<List<RecordJoined>> = if (searchTerm.isNullOrEmpty()) {
                recordsDAO.getFlow(sinceEpochMillis)
            } else {
                recordsDAO.getFlowBySearchTerm(searchTerm)
            }
            raw
                .distinctUntilChanged()
                .map(mapper::map)
        } catch (t: Throwable) {
            analyticsLogger.logError(t)
            flowOf(emptyList())
        }
    }

    override suspend fun get(recordId: Long): Record? {
        return recordsDAO.getById(recordId)?.let(mapper::map)
    }

    override fun observe(recordId: Long): Flow<Record> {
        return recordsDAO.observe(recordId).map(mapper::map)
    }

    override suspend fun getTemplate(templateId: Long): Template = templatesDAO
        .getTemplateById(templateId)
        .let(mapper::map)

    // -------- Writes --------

    override suspend fun saveTemplate(templateToSave: TemplateToSave): Long {
        val template = mapper.map(templateToSave)
        val templateId = templatesDAO.insertOrUpdate(template)
        templatesDAO.deleteAllImagesForTemplate(templateId)
        templateToSave.images.forEachIndexed { index, image ->
            templatesDAO.upsertImage(
                ImageEntity(
                    templateId = templateId,
                    image = image,
                    sortOrder = index,
                )
            )
        }
        return templateId
    }

    override suspend fun saveRecord(templateId: Long, timestamp: ZonedDateTime): Long {
        return recordsDAO.insertOrUpdate(mapper.map(templateId, timestamp))
    }

    override suspend fun updateRecord(record: Record) {
        recordsDAO.insertOrUpdate(
            RecordEntity(
                timestamp = record.timestamp.toLocalDateTime(),
                zoneId = record.timestamp.zone.id,
                epochMillis = record.timestamp.toInstant().toEpochMilli(),
                templateId = record.template.dbId,
            ).apply {
                id = record.recordId
            }
        )
    }

    override suspend fun deleteRecord(recordId: Long): Record {
        val recordJoined = recordsDAO.getById(recordId)!!
        recordsDAO.delete(recordId)
        return mapper.map(recordJoined)
    }

    override suspend fun updateTemplate(
        templateId: Long,
        name: String?, /* = null */
        description: String?, /* = null */
        images: List<String>?, /* = null */
        coverPhotoByImageIndex: List<Boolean?>?,
        nutrients: Pair<TemplateNutrientBreakdown, TopContributors>?, /* = null */
        notes: String?, /* = null */
        mealComponents: List<MealComponent>?,
    ) {
        val oldTemplate = templatesDAO.getTemplateById(templateId)

        templatesDAO.insertOrUpdate(
            TemplateEntity(
                name = name ?: oldTemplate.entity.name,
                description = description ?: oldTemplate.entity.description,
            ).apply { id = templateId }
        )

        if (name == null && description == null && images == null && nutrients == null && notes == null && mealComponents == null && coverPhotoByImageIndex == null) {
            return
        }

        images?.let {
            templatesDAO.deleteAllImagesForTemplate(templateId)
            images.forEachIndexed { index, image ->
                templatesDAO.upsertImage(
                    ImageEntity(
                        templateId = templateId,
                        image = image,
                        sortOrder = index,
                        coverPhoto = coverPhotoByImageIndex?.getOrNull(index),
                    )
                )
            }
        }

        if (nutrients != null) {
            val (nutrients, topContributors) = nutrients
            val componentsJson = when {
                mealComponents != null -> encodeMealComponentsJson(mealComponents)
                else -> oldTemplate.macros?.analysisComponentsJson
            }
            val macrosEntity: MacrosEntity = mapper.map(
                nutrientBreakdown = nutrients,
                notes = notes ?: oldTemplate.macros?.notes,
                analysisComponentsJson = componentsJson,
                id = oldTemplate.macros?.id,
                templateId = templateId,
            )
            templatesDAO.insertOrUpdate(macrosEntity)

            val topContributorsEntity: TopContributorsEntity = mapper.map(
                topContributors = topContributors,
                id = oldTemplate.topContributors?.id,
                templateId = templateId
            )
            templatesDAO.insertOrUpdate(topContributorsEntity)

            if (coverPhotoByImageIndex != null) {
                val rows = templatesDAO.getImagesForTemplate(templateId).sortedBy { it.sortOrder }
                rows.forEachIndexed { index, row ->
                    templatesDAO.upsertImage(
                        row.copy(coverPhoto = coverPhotoByImageIndex.getOrNull(index))
                    )
                }
            }
        }
    }

    override suspend fun deleteTemplateIfUnused(
        templateId: Long,
        imageToo: Boolean,
    ): Pair<Boolean, Boolean> {
        return if (recordsDAO.getByTemplate(templateId).isEmpty()) { // template is unused
            val images = templatesDAO.getImagesForTemplate(templateId)
            val documentDeleted = templatesDAO.delete(templateId) > 0
            val imagesDeleted = if (imageToo) {
                // try to delete each backing file if no longer referenced elsewhere
                images.fold(false) { acc, img -> deleteImageIfUnused(img.image) || acc }
            } else false
            Pair(documentDeleted, imagesDeleted)
        } else {
            Pair(false, false)
        }
    }

    override suspend fun addQuickPickOverride(templateId: Long, type: Template.QuickPickOverride) {
        val entityType = QuickPickOverrideEntity.OverrideType.valueOf(type.name)
        templatesDAO.upsertQuickPickOverride(
            QuickPickOverrideEntity(templateId = templateId, overrideType = entityType)
        )
    }

    override suspend fun removeQuickPickOverride(templateId: Long) {
        templatesDAO.deleteQuickPickOverride(templateId)
    }

    private suspend fun deleteImageIfUnused(image: String): Boolean {
        val refs = templatesDAO.countTemplatesByImage(image)
        if (refs == 0) {
            imageStore.delete(image)
            return true
        }
        return false
    }
}
