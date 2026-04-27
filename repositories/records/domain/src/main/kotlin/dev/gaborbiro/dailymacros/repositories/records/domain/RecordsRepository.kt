package dev.gaborbiro.dailymacros.repositories.records.domain

import dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime

interface RecordsRepository {

    suspend fun getRecords(since: ZonedDateTime? = null): List<Record>

    /**
     * Most recent [limit] records (newest first), for batch jobs such as variability mining.
     */
    suspend fun getRecentRecords(limit: Int): List<Record>

    fun getMostRecentRecord(): Record?

    suspend fun getQuickPicks(count: Int): List<Template>

    suspend fun getRecordsByTemplate(templateId: Long): List<Record>

    suspend fun countRecordsForTemplate(templateId: Long): Int

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
        /** When [images] is non-null; null entry = DB null; param null = all DB null for new rows. */
        isRepresentativeOfMealByImageIndex: List<Boolean?>? = null,
        nutrients: Pair<TemplateNutrientBreakdown, TopContributors>? = null,
        notes: String? = null,
        /** When non-null and [nutrients] is non-null, replaces stored analysis components. When null, preserves existing JSON. */
        mealComponents: List<MealComponent>? = null,
    )

    suspend fun addQuickPickOverride(templateId: Long, type: Template.QuickPickOverride)

    suspend fun removeQuickPickOverride(templateId: Long)
}
