package dev.gaborbiro.nutri.data.records

import androidx.room.Transaction
import dev.gaborbiro.nutri.data.records.domain.RecordsRepository
import dev.gaborbiro.nutri.data.records.domain.model.Nutrients
import dev.gaborbiro.nutri.data.records.domain.model.Record
import dev.gaborbiro.nutri.data.records.domain.model.RecordToSave
import dev.gaborbiro.nutri.data.records.domain.model.Template
import dev.gaborbiro.nutri.store.db.records.RecordsDAO
import dev.gaborbiro.nutri.store.db.records.TemplatesDAO
import dev.gaborbiro.nutri.store.db.records.model.RecordDBModel
import dev.gaborbiro.nutri.store.db.records.model.TemplateDBModel
import dev.gaborbiro.nutri.store.file.FileStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class RecordsRepositoryImpl(
    private val templatesDAO: TemplatesDAO,
    private val recordsDAO: RecordsDAO,
    private val mapper: DBMapper,
    private val fileStore: FileStore,
) : RecordsRepository {

    override suspend fun getRecords(since: LocalDateTime? /* = null */): List<Record> {
        val records = recordsDAO.get()
        val filteredRecords = since
            ?.let {
                records.filter { it.record.timestamp >= since }
            } ?: records
        return mapper.map(filteredRecords)
    }

    override suspend fun getTemplatesByFrequency(): List<Template> {
        return templatesDAO.getByFrequency().map(mapper::map)
    }

    override suspend fun getRecordsByTemplate(templateId: Long): List<Record> {
        return mapper.map(recordsDAO.getByTemplate(templateId))
    }

    override fun getFlowBySearchTerm(search: String? /* = null */): Flow<List<Record>> {
        return try {
            val raw = if (search.isNullOrEmpty()) {
                recordsDAO.getFlow()
            } else {
                recordsDAO.getFlowBySearchTerm(search)
            }
            raw
                .distinctUntilChanged()
                .map(mapper::map)
        } catch (t: Throwable) {
            flowOf(emptyList())
        }
    }

    override suspend fun saveRecord(record: RecordToSave): Long {
        val templateId = templatesDAO.insertOrUpdate(mapper.map(record.template))
        return recordsDAO.insertOrUpdate(mapper.map(record, templateId))
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
        return getRecord(recordId)!!.let { record ->
            recordsDAO.insertOrUpdate(mapper.map(record, LocalDateTime.now()))
        }
    }

    @Transaction
    override suspend fun getRecord(recordId: Long): Record? {
        return recordsDAO.get(recordId)?.let(mapper::map)
    }

    override suspend fun deleteRecord(recordId: Long): Record {
        val record = recordsDAO.get(recordId)!!
        recordsDAO.delete(recordId)
        return mapper.map(record)
    }

    override suspend fun applyTemplate(templateId: Long): Long {
        return recordsDAO.insertOrUpdate(RecordDBModel(LocalDateTime.now(), templateId))
    }

    override suspend fun updateTemplate(
        templateId: Long,
        image: String?,/* = null */
        title: String?,/* = null */
        description: String?,/* = null */
        nutrients: Nutrients?,
    ) {
        templatesDAO.get(templateId)?.let { oldTemplate ->
            templatesDAO.insertOrUpdate(
                TemplateDBModel(
                    id = templateId,
                    image = image ?: oldTemplate.image,
                    name = title ?: oldTemplate.name,
                    description = description ?: oldTemplate.description,
                    calories = nutrients?.calories ?: oldTemplate.calories,
                    protein = nutrients?.protein ?: oldTemplate.protein,
                    carbohydrates = nutrients?.carbohydrates ?: oldTemplate.carbohydrates,
                    fat = nutrients?.fat ?: oldTemplate.fat,
                )
            )
            oldTemplate.image?.let { deleteImageIfUnused(it) }
        }
    }

    override suspend fun deleteImage(templateId: Long) {
        templatesDAO.get(templateId)?.let { oldTemplate ->
            templatesDAO.insertOrUpdate(
                TemplateDBModel(
                    id = templateId,
                    image = null,
                    name = oldTemplate.name,
                    description = oldTemplate.description,
                )
            )
            oldTemplate.image?.let { deleteImageIfUnused(it) }
        }
    }

    override suspend fun deleteTemplateIfUnused(
        templateId: Long,
        imageToo: Boolean,
    ): Pair<Boolean, Boolean> {
        return if (recordsDAO.getByTemplate(templateId).isEmpty()) { // template is unused
            val image = templatesDAO.get(templateId)!!.image
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
        if (templatesDAO.get(image).isEmpty()) {
            fileStore.delete(image)
        }
        return false
    }
}
