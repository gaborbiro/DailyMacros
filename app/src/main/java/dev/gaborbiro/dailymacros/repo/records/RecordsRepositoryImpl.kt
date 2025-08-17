package dev.gaborbiro.dailymacros.repo.records

import androidx.room.Transaction
import dev.gaborbiro.dailymacros.data.db.records.RecordsDAO
import dev.gaborbiro.dailymacros.data.db.records.TemplatesDAO
import dev.gaborbiro.dailymacros.data.db.records.model.RecordDBModel
import dev.gaborbiro.dailymacros.data.db.records.model.TemplateDBModel
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

    override suspend fun getRecords(since: LocalDateTime? /* = null */): List<Record> {
        val records = recordsDAO.get()
        val filteredRecords = since
            ?.let {
                records.filter { it.record.timestamp >= since }
            } ?: records
        return dBMapper.map(filteredRecords)
    }

    override suspend fun getRecordsFlow(
        since: LocalDateTime,
    ): Flow<List<Record>> {
        return try {
            recordsDAO
                .getFlow(since)
                .map { dBMapper.map(it) }
        } catch (t: Throwable) {
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
        } catch (t: Throwable) {
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
                recordsDAO.getFlow()
            } else {
                recordsDAO.getFlowBySearchTerm(search)
            }
            raw
                .distinctUntilChanged()
                .map(dBMapper::map)
        } catch (t: Throwable) {
            flowOf(emptyList())
        }
    }

    override suspend fun saveRecord(record: RecordToSave): Long {
        val templateId = templatesDAO.insertOrUpdate(dBMapper.map(record.template))
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
        return getRecord(recordId)!!.let { record ->
            recordsDAO.insertOrUpdate(dBMapper.map(record, LocalDateTime.now()))
        }
    }

    @Transaction
    override suspend fun getRecord(recordId: Long): Record? {
        return recordsDAO.get(recordId)?.let(dBMapper::map)
    }

    override suspend fun getTemplate(templateId: Long): Template? {
        return templatesDAO.get(templateId)?.let(dBMapper::map)
    }

    override suspend fun deleteRecord(recordId: Long): Record {
        val record = recordsDAO.get(recordId)!!
        recordsDAO.delete(recordId)
        return dBMapper.map(record)
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
                if (nutrients == null) {
                    TemplateDBModel(
                        id = templateId,
                        image = image ?: oldTemplate.image,
                        name = title ?: oldTemplate.name,
                        description = description ?: oldTemplate.description,
                        calories = oldTemplate.calories,
                        protein = oldTemplate.protein,
                        carbohydrates = oldTemplate.carbohydrates,
                        ofWhichSugar = oldTemplate.ofWhichSugar,
                        fat = oldTemplate.fat,
                        ofWhichSaturated = oldTemplate.ofWhichSaturated,
                        salt = oldTemplate.salt,
                        fibre = oldTemplate.fibre,
                    )
                } else {
                    TemplateDBModel(
                        id = templateId,
                        image = image ?: oldTemplate.image,
                        name = title ?: oldTemplate.name,
                        description = description ?: oldTemplate.description,
                        calories = nutrients.calories,
                        protein = nutrients.protein,
                        carbohydrates = nutrients.carbohydrates,
                        ofWhichSugar = nutrients.ofWhichSugar,
                        fat = nutrients.fat,
                        ofWhichSaturated = nutrients.ofWhichSaturated,
                        salt = nutrients.salt,
                        fibre = nutrients.fibre,
                    )
                }
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
            imageStore.delete(image)
        }
        return false
    }
}
