package dev.gaborbiro.dailymacros.repo.records

import androidx.room.Transaction
import dev.gaborbiro.dailymacros.data.db.RecordsDAO
import dev.gaborbiro.dailymacros.data.db.TemplatesDAO
import dev.gaborbiro.dailymacros.data.db.model.entity.ImageEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.MacrosEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.RecordEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TemplateEntity
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repo.records.domain.model.Macros
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.records.domain.model.RecordToSave
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

internal class RecordsRepositoryImpl(
    private val templatesDAO: TemplatesDAO,
    private val recordsDAO: RecordsDAO,
    private val mapper: ApiMapper,
    private val imageStore: ImageStore,
) : RecordsRepository {

    // -------- Reads --------

    override suspend fun getRecords(since: LocalDateTime): List<Record> = recordsDAO
        .get()
        .filter { it.entity.timestamp >= since }
        .map(mapper::map)

    override suspend fun getRecordsFlow(
        since: LocalDateTime,
        until: LocalDateTime?,
    ): Flow<List<Record>> = recordsDAO
        .let { dao ->
            until
                ?.let { dao.getFlow(since, until) }
                ?: dao.getFlow(since)
        }
        .map(mapper::map)

    override suspend fun getTop10(): List<Template> = templatesDAO
        .getTop10()
        .map(mapper::map)

    override suspend fun getRecordsByTemplate(templateId: Long): List<Record> = recordsDAO
        .getByTemplate(templateId)
        .let(mapper::map)

    override fun getFlowBySearchTerm(search: String? /* = null */): Flow<List<Record>> {
        return try {
            val raw = if (search.isNullOrEmpty()) {
                recordsDAO.getFlow(LocalDateTime.MIN)
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
    override suspend fun getRecord(recordId: Long): Record {
        return recordsDAO.get(recordId).let(mapper::map)
    }

    override suspend fun getTemplate(templateId: Long): Template? = templatesDAO
        .getTemplateById(templateId)
        .let(mapper::map)

    // -------- Writes --------

    override suspend fun saveRecord(record: RecordToSave): Long {
        val template = mapper.map(record.templateToSave)
        val rowId = templatesDAO.insertOrUpdate(template)
        val templateId = if (rowId == -1L) {
            requireNotNull(template.id) { "Template id must be set when updating" }
        } else rowId
        templatesDAO.deleteAllImagesForTemplate(templateId)
        record.templateToSave.images.forEachIndexed { index, image ->
            templatesDAO.upsertImage(
                ImageEntity(
                    templateId = templateId,
                    image = image,
                    sortOrder = index,
                )
            )
        }
        return recordsDAO.insertOrUpdate(mapper.map(record, templateId))
    }

    override suspend fun updateRecord(record: Record) {
        recordsDAO.insertOrUpdate(
            RecordEntity(
                timestamp = record.timestamp,
                templateId = record.template.dbId,
            ).apply {
                id = record.dbId
            }
        )
    }

    override suspend fun duplicateRecord(recordId: Long): Long {
        val record = getRecord(recordId)
        return recordsDAO.insertOrUpdate(mapper.map(record, LocalDateTime.now()))
    }

    override suspend fun deleteRecord(recordId: Long): Record {
        val recordJoined = recordsDAO.get(recordId)
        recordsDAO.delete(recordId)
        return mapper.map(recordJoined)
    }

    override suspend fun applyTemplate(templateId: Long): Long {
        return recordsDAO.insertOrUpdate(RecordEntity(LocalDateTime.now(), templateId))
    }

    override suspend fun updateTemplate(
        templateId: Long,
        title: String?, /* = null */
        description: String?, /* = null */
        images: List<String>?,
        macros: Macros?,
    ) {
        val oldTemplate = templatesDAO.getTemplateById(templateId)

        templatesDAO.insertOrUpdate(
            TemplateEntity(
                name = title ?: oldTemplate.entity.name,
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
