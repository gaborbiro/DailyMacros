package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class RepeatRecordUseCaseTest {

    private val zone = ZoneId.of("UTC")

    private fun stubTemplate(dbId: Long) = Template(
        dbId = dbId,
        images = emptyList(),
        isRepresentativeOfMealByImageIndex = emptyList(),
        name = "Meal",
        description = "D",
        parentTemplateId = null,
        createdAtEpochMs = 0L,
        updatedAtEpochMs = 0L,
        isPending = false,
        nutrients = TemplateNutrientBreakdown(),
        notes = "",
        mealComponents = emptyList(),
        topContributors = TopContributors(),
        quickPickOverride = null,
    )

    @Test
    fun `execute loads record and creates from its template id`() = runBlocking {
        val record = Record(
            recordId = 100L,
            timestamp = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, zone),
            template = stubTemplate(dbId = 33L),
        )
        var savedTemplateId: Long? = null
        val repo = object : StubRecordsRepository() {
            override suspend fun get(recordId: Long): Record? =
                if (recordId == 100L) record else null

            override suspend fun saveRecord(templateId: Long, timestamp: ZonedDateTime): Long {
                savedTemplateId = templateId
                return 999L
            }
        }
        val create = CreateRecordFromTemplateUseCase(repo)
        val id = RepeatRecordUseCase(repo, create).execute(100L)
        assertEquals(999L, id)
        assertEquals(33L, savedTemplateId)
    }

    private open class StubRecordsRepository : RecordsRepository {
        override suspend fun getRecords(since: ZonedDateTime?) = emptyList<Record>()
        override suspend fun getRecentRecords(limit: Int) = emptyList<Record>()
        override suspend fun getRecordsForVariabilityDelta(limit: Int, afterWatermarkExclusive: Long) =
            emptyList<Record>()
        override suspend fun countTemplates(): Int = 0
        override suspend fun countTemplatesPendingVariabilityAfterWatermark(afterWatermarkExclusive: Long) = 0
        override fun getMostRecentRecord(): Record? = null
        override suspend fun getQuickPicks(count: Int) = emptyList<Template>()
        override suspend fun getRecordsByTemplate(templateId: Long) = emptyList<Record>()
        override suspend fun countRecordsForTemplate(templateId: Long): Int = 0
        override fun observeRecords(searchTerm: String?, sinceEpochMillis: Long) = emptyFlow<List<Record>>()
        override suspend fun get(recordId: Long): Record? = null
        override fun observe(recordId: Long): Flow<Record> = emptyFlow()
        override suspend fun getTemplate(templateId: Long): Template = error("not used")
        override suspend fun saveTemplate(templateToSave: TemplateToSave) = 0L
        override suspend fun saveRecord(templateId: Long, timestamp: ZonedDateTime) = 0L
        override suspend fun updateRecord(record: Record) = Unit
        override suspend fun deleteRecord(recordId: Long): Record = error("not used")
        override suspend fun deleteTemplateIfUnused(templateId: Long, imageToo: Boolean) = false to false
        override suspend fun updateTemplate(
            templateId: Long,
            name: String?,
            description: String?,
            templateImages: List<TemplateImageUpdate>?,
            nutrients: Pair<TemplateNutrientBreakdown, TopContributors>?,
            notes: String?,
            mealComponents: List<MealComponent>?,
        ) = Unit
        override suspend fun addQuickPickOverride(templateId: Long, type: Template.QuickPickOverride) = Unit
        override suspend fun removeQuickPickOverride(templateId: Long) = Unit
    }
}
