package dev.gaborbiro.dailymacros.repo.records.domain

import dev.gaborbiro.dailymacros.repo.records.domain.model.Nutrients
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.records.domain.model.RecordToSave
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

internal interface RecordsRepository {

    suspend fun getRecords(since: LocalDateTime? = null): List<Record>

    suspend fun getRecordsFlow(since: LocalDateTime): Flow<List<Record>>

    suspend fun getRecordsFlow(since: LocalDateTime, until: LocalDateTime): Flow<List<Record>>

    suspend fun getTemplatesByFrequency(): List<Template>

    suspend fun getRecordsByTemplate(templateId: Long): List<Record>

    fun getFlowBySearchTerm(search: String? = null): Flow<List<Record>>

    suspend fun getRecord(recordId: Long): Record?

    suspend fun getTemplate(templateId: Long): Template?

    suspend fun saveRecord(record: RecordToSave): Long

    suspend fun updateRecord(record: Record)

    suspend fun duplicateRecord(recordId: Long): Long

    suspend fun applyTemplate(templateId: Long): Long

    suspend fun deleteRecord(recordId: Long): Record

    /**
     * @return whether the template and image have been deleted
     */
    suspend fun deleteTemplateIfUnused(
        templateId: Long,
        imageToo: Boolean,
    ): Pair<Boolean, Boolean>

    /**
     * null means value is not changed
     */
    suspend fun updateTemplate(
        templateId: Long,
        image: String? = null,
        title: String? = null,
        description: String? = null,
        nutrients: Nutrients? = null,
    )

    suspend fun deleteImage(templateId: Long)
}
