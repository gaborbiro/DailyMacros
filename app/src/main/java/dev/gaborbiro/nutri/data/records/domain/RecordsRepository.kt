package dev.gaborbiro.nutri.data.records.domain

import dev.gaborbiro.nutri.data.records.DBMapper
import dev.gaborbiro.nutri.data.records.RecordsRepositoryImpl
import dev.gaborbiro.nutri.data.records.domain.model.Nutrients
import dev.gaborbiro.nutri.data.records.domain.model.Record
import dev.gaborbiro.nutri.data.records.domain.model.Template
import dev.gaborbiro.nutri.data.records.domain.model.RecordToSave
import dev.gaborbiro.nutri.store.db.AppDatabase
import dev.gaborbiro.nutri.store.file.FileStore
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface RecordsRepository {

    companion object {

        private lateinit var INSTANCE: RecordsRepository

        fun get(fileStore: FileStore): RecordsRepository {
            if (!::INSTANCE.isInitialized) {
                INSTANCE = RecordsRepositoryImpl(
                    templatesDAO = AppDatabase.getInstance().templatesDAO(),
                    recordsDAO = AppDatabase.getInstance().recordsDAO(),
                    mapper = DBMapper.get(),
                    fileStore = fileStore,
                )
            }
            return INSTANCE
        }
    }

    suspend fun getRecords(since: LocalDateTime? = null): List<Record>

    suspend fun getTemplatesByFrequency(): List<Template>

    suspend fun getRecordsByTemplate(templateId: Long): List<Record>

    fun getFlowBySearchTerm(search: String? = null): Flow<List<Record>>

    suspend fun getRecord(recordId: Long): Record?

    suspend fun saveRecord(record: RecordToSave): Long

    suspend fun updateRecord(record: Record)

    suspend fun duplicateRecord(recordId: Long): Long

    suspend fun applyTemplate(templateId: Long): Long

    suspend fun deleteRecord(recordId: Long): Record

    /**
     * @return whether the template and image have been deleted
     */
    suspend fun deleteTemplateIfUnused(templateId: Long, imageToo: Boolean = true): Pair<Boolean, Boolean>

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
