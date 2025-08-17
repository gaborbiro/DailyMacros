package dev.gaborbiro.dailymacros.repo.records

import androidx.room.Transaction
import dev.gaborbiro.dailymacros.data.db.RecordsDAO
import dev.gaborbiro.dailymacros.data.db.TemplatesDAO
import dev.gaborbiro.dailymacros.data.db.model.RecordDBModel
import dev.gaborbiro.dailymacros.data.db.model.TemplateDBModel
import dev.gaborbiro.dailymacros.data.db.model.TemplateWithNutrients
import dev.gaborbiro.dailymacros.data.image.ImageStore
import dev.gaborbiro.dailymacros.repo.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repo.records.domain.model.Nutrients
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
    private val dBMapper: DBMapper,
    private val imageStore: ImageStore,
) : RecordsRepository {

    // -------- Reads --------

    override suspend fun getRecords(since: LocalDateTime? /* = null */): List<Record> {
        val records = recordsDAO.get()
        val filteredRecords = since
            ?.let {
                records.filter { it.record.timestamp >= since }
            }
            ?: records
        return dBMapper.map(filteredRecords)
    }

    override suspend fun getRecordsFlow(
        since: LocalDateTime,
    ): Flow<List<Record>> {
        return try {
            recordsDAO
                .getFlow(since)
                .map { dBMapper.map(it) }
        } catch (_: Throwable) {
            flowOf(emptyList())
        }
    }

    override suspend fun getRecordsFlow(
        since: LocalDateTime,
        until: LocalDateTime,
    ): Flow<List<Record>> {
        return try {
            recordsDAO
                .getFlow(since, until)
                .map { dBMapper.map(it) }
        } catch (_: Throwable) {
            flowOf(emptyList())
        }
    }

    override suspend fun getTemplatesByFrequency(): List<Template> {
        return templatesDAO.getByFrequency().map(dBMapper::map)
    }

    override suspend fun getRecordsByTemplate(templateId: Long): List<Record> {
        return dBMapper.map(recordsDAO.getByTemplate(templateId))
    }

    override fun getFlowBySearchTerm(search: String? /* = null */): Flow<List<Record>> {
        return try {
            val raw = if (search.isNullOrEmpty()) {
                recordsDAO.getFlow(LocalDateTime.MIN)
            } else {
                recordsDAO.getFlowBySearchTerm(search)
            }
            raw
                .distinctUntilChanged()
                .map(dBMapper::map)
        } catch (_: Throwable) {
            flowOf(emptyList())
        }
    }

    @Transaction
    override suspend fun getRecord(recordId: Long): Record {
        return recordsDAO.get(recordId).let(dBMapper::map)
    }

    override suspend fun getTemplate(templateId: Long): Template? {
        val (template, nutrients) = templatesDAO.get(templateId)
        return dBMapper.map(template, nutrients)
    }

    // -------- Writes --------

    override suspend fun saveRecord(record: RecordToSave): Long {
        val template = dBMapper.map(record.template)
        val rowId = templatesDAO.insertOrUpdate(template)
        val templateId = if (rowId == -1L) {
            requireNotNull(template.id) { "Template id must be set when updating" }
        } else rowId
        return recordsDAO.insertOrUpdate(dBMapper.map(record, templateId))
    }

    override suspend fun updateRecord(record: Record) {
        recordsDAO.insertOrUpdate(
            RecordDBModel(
                timestamp = record.timestamp,
                templateId = record.template.id,
            ).apply {
                id = record.id
            }
        )
    }

    override suspend fun duplicateRecord(recordId: Long): Long {
        val record = getRecord(recordId)
        return recordsDAO.insertOrUpdate(dBMapper.map(record, LocalDateTime.now()))
    }

    override suspend fun deleteRecord(recordId: Long): Record {
        val recordWithTemplateAndNutrients = recordsDAO.get(recordId)
        recordsDAO.delete(recordId)
        return dBMapper.map(recordWithTemplateAndNutrients)
    }

    override suspend fun applyTemplate(templateId: Long): Long {
        return recordsDAO.insertOrUpdate(RecordDBModel(LocalDateTime.now(), templateId))
    }

    override suspend fun updateTemplate(
        templateId: Long,
        image: String?, /* = null */
        title: String?, /* = null */
        description: String?, /* = null */
        nutrients: Nutrients?,
    ) {
        val oldTemplate = templatesDAO.get(templateId)

        templatesDAO.insertOrUpdate(
            TemplateDBModel(
                image = image ?: oldTemplate.template.image,
                name = title ?: oldTemplate.template.name,
                description = description ?: oldTemplate.template.description,
            ).apply { id = templateId }
        )

        if (nutrients == null) {
            templatesDAO.deleteNutrientsForTemplate(templateId)
        } else {
            val nutrientsDBModel = dBMapper.map(nutrients).copy(templateId = templateId)
            templatesDAO.insertOrUpdate(nutrientsDBModel)
        }

        oldTemplate.template.image?.let { deleteImageIfUnused(it) }
    }

    override suspend fun deleteImage(templateId: Long) {
        val oldTemplate = templatesDAO.get(templateId)
        templatesDAO.insertOrUpdate(
            TemplateDBModel(
                image = null,
                name = oldTemplate.template.name,
                description = oldTemplate.template.description,
            ).apply {
                id = templateId
            }
        )
        oldTemplate.template.image?.let { deleteImageIfUnused(it) }
    }

    override suspend fun deleteTemplateIfUnused(
        templateId: Long,
        imageToo: Boolean,
    ): Pair<Boolean, Boolean> {
        return if (recordsDAO.getByTemplate(templateId).isEmpty()) { // template is unused
            val image = templatesDAO.get(templateId).template.image
            val documentDeleted = templatesDAO.delete(templateId) > 0
            val imageDeleted = if (imageToo) {
                image
                    ?.let {
                        deleteImageIfUnused(it)
                    } == true
            } else false
            Pair(documentDeleted, imageDeleted)
        } else {
            Pair(false, false)
        }
    }

    private suspend fun deleteImageIfUnused(image: String): Boolean {
        if (templatesDAO.getByImage(image).isEmpty()) {
            imageStore.delete(image)
        }
        return false
    }
}
