package dev.gaborbiro.dailymacros.repositories.records.domain

import dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime

interface RecordsRepository {

    suspend fun getRecords(since: ZonedDateTime? = null): List<Record>

    /**
     * Most recent [limit] records (newest first), for batch jobs such as variability mining.
     *
     * Used for the **first** meal-variability mine (no snapshot yet) and for mines where the
     * stored snapshot has [MealVariabilityProfileSnapshot.templatesIngestWatermarkEpochMs] == 0
     * (legacy / not yet populated): the worker sends a bounded window of observations, not a
     * template-timestamp delta.
     */
    suspend fun getRecentRecords(limit: Int): List<Record>

    /**
     * Records whose backing template has **strictly newer** activity than [afterWatermarkExclusive]
     * (epoch ms): `max(createdAtEpochMs, updatedAtEpochMs) > afterWatermarkExclusive`, newest
     * record first, capped by [limit].
     *
     * Semantics match [countTemplatesPendingVariabilityAfterWatermark]: the watermark is
     * **exclusive** — templates whose timestamps equal the watermark are **not** included.
     *
     * Intended caller: incremental variability mining after a snapshot exists with a **positive**
     * [MealVariabilityProfileSnapshot.templatesIngestWatermarkEpochMs]. When that watermark is
     * still 0, the mining use case uses [getRecentRecords] instead so the first post-upgrade run
     * is a full observation window, not an empty delta.
     */
    suspend fun getRecordsForVariabilityDelta(limit: Int, afterWatermarkExclusive: Long): List<Record>

    suspend fun countTemplates(): Int

    /**
     * Count of templates that would contribute rows to [getRecordsForVariabilityDelta] for the same
     * [afterWatermarkExclusive]: `createdAtEpochMs > afterWatermarkExclusive OR updatedAtEpochMs >
     * afterWatermarkExclusive` (strictly greater on both sides — **exclusive** watermark).
     *
     * Exposed for settings UX (“templates changed since last mine”). When
     * [afterWatermarkExclusive] is 0, the SQL still requires a timestamp **> 0**, so templates
     * that only have legacy 0/0 timestamps do not count as “pending” until they are touched.
     */
    suspend fun countTemplatesPendingVariabilityAfterWatermark(afterWatermarkExclusive: Long): Int

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
        /** When non-null, replaces all image rows for the template (order = list order). */
        templateImages: List<TemplateImageUpdate>? = null,
        nutrients: Pair<TemplateNutrientBreakdown, TopContributors>? = null,
        notes: String? = null,
        /** When non-null and [nutrients] is non-null, replaces stored analysis components. When null, preserves existing JSON. */
        mealComponents: List<MealComponent>? = null,
    )

    suspend fun addQuickPickOverride(templateId: Long, type: Template.QuickPickOverride)

    suspend fun removeQuickPickOverride(templateId: Long)
}
