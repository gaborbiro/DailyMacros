package dev.gaborbiro.dailymacros.repo.records.domain

import dev.gaborbiro.dailymacros.data.db.model.entity.QuickPickOverrideEntity
import dev.gaborbiro.dailymacros.repo.records.domain.model.Record
import dev.gaborbiro.dailymacros.repo.records.domain.model.Template
import dev.gaborbiro.dailymacros.repo.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repo.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.repo.records.domain.model.TopContributors
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime

internal interface RecordsRepository {

    suspend fun getRecords(since: ZonedDateTime? = null): List<Record>

    fun getMostRecentRecord(): Record?

    suspend fun getQuickPicks(count: Int): List<Template>

    suspend fun getRecordsByTemplate(templateId: Long): List<Record>

    fun observeRecords(searchTerm: String? = null, sinceEpochMillis: Long = 0L): Flow<List<Record>>

    suspend fun get(recordId: Long): Record?

    fun observe(recordId: Long): Flow<Record>

    suspend fun getTemplate(templateId: Long): Template

    suspend fun saveTemplate(templateToSave: TemplateToSave): Long

    suspend fun saveRecord(templateId: Long, timestamp: ZonedDateTime): Long

    suspend fun updateRecord(record: Record)

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
        name: String? = null,
        description: String? = null,
        images: List<String>? = null,
        nutrients: Pair<TemplateNutrientBreakdown, TopContributors>? = null,
        notes: String? = null,
    )

    suspend fun addQuickPickOverride(templateId: Long, type: QuickPickOverrideEntity.OverrideType)

    suspend fun removeQuickPickOverride(templateId: Long)
}
