package dev.gaborbiro.dailymacros.repo.records

import androidx.room.Transaction
import dev.gaborbiro.dailymacros.data.db.RecordsDAO
import dev.gaborbiro.dailymacros.data.db.TemplatesDAO
import dev.gaborbiro.dailymacros.data.db.model.RecordJoined
import dev.gaborbiro.dailymacros.data.db.model.entity.ImageEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.MacrosEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.RecordEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TemplateEntity
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repo.records.domain.model.Macros
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template
import dev.gaborbiro.dailymacros.repo.records.domain.model.TemplateToSave
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.ZonedDateTime

internal class RecordsRepositoryImpl(
    private val templatesDAO: TemplatesDAO,
    private val recordsDAO: RecordsDAO,
    private val mapper: RecordsApiMapper,
    private val imageStore: ImageStore,
) : RecordsRepository {

    // -------- Reads --------

    override suspend fun getRecords(since: ZonedDateTime): List<Record> = recordsDAO
        .get(since.toInstant().toEpochMilli())
        .map(mapper::map)

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

    override fun getFlowBySearchTerm(search: String? /* = null */): Flow<List<Record>> {
        return try {
            val raw: Flow<List<RecordJoined>> = if (search.isNullOrEmpty()) {
                recordsDAO.getFlow(0)
            } else {
                recordsDAO.getFlowBySearchTerm(search)
            }
            raw
                .distinctUntilChanged()
                .map(mapper::map)
        } catch (t: Throwable) {
            t.printStackTrace()
            flowOf(emptyList())
        }
    }

    @Transaction
    override suspend fun get(recordId: Long): Record? {
        return recordsDAO.getById(recordId)?.let(mapper::map)
    }

    @Transaction
    override fun observe(recordId: Long): Flow<Record> {
        return recordsDAO.observe(recordId).map(mapper::map)
    }

    override suspend fun getTemplate(templateId: Long): Template? = templatesDAO
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
        images: List<String>?,
        macros: Macros?,
    ) {
        val oldTemplate = templatesDAO.getTemplateById(templateId)

        templatesDAO.insertOrUpdate(
            TemplateEntity(
                name = name ?: oldTemplate.entity.name,
                description = description ?: oldTemplate.entity.description,
            ).apply { id = templateId }
        )

        images?.let {
            templatesDAO.deleteAllImagesForTemplate(templateId)
            images.forEachIndexed { index, image ->
                templatesDAO.upsertImage(
                    ImageEntity(
                        templateId = templateId,
                        image = image,
                        sortOrder = index,
                    )
                )
            }
        }

        if (macros == null) {
            templatesDAO.deleteMacrosForTemplate(templateId)
        } else {
            val entity: MacrosEntity = mapper.map(
                macros = macros,
                id = oldTemplate.macros?.id,
                templateId = templateId
            )
            templatesDAO.insertOrUpdate(entity)
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

    private suspend fun deleteImageIfUnused(image: String): Boolean {
        val refs = templatesDAO.countTemplatesByImage(image)
        if (refs == 0) {
            imageStore.delete(image)
            return true
        }
        return false
    }
}
