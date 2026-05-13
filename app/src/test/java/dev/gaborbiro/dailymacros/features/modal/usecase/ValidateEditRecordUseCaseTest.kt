package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ValidateEditRecordUseCaseTest {

    private val zone = ZoneId.of("UTC")

    private fun stubTemplate(
        dbId: Long = 1L,
        name: String = "Same",
        description: String = "Desc",
    ) = Template(
        dbId = dbId,
        images = emptyList(),
        isRepresentativeOfMealByImageIndex = emptyList(),
        name = name,
        description = description,
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

    private fun stubRecord(recordId: Long, template: Template) = Record(
        recordId = recordId,
        timestamp = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, zone),
        template = template,
    )

    @Test
    fun `returns error when record missing`() = runBlocking {
        val repo = object : StubRecordsRepository() {
            override suspend fun get(recordId: Long): Record? = null
        }
        val result = ValidateEditRecordUseCase(repo).execute(99L, "Title", "Desc")
        assertTrue(result is EditValidationResult.Error)
        assertEquals("Record not found", (result as EditValidationResult.Error).message)
    }

    @Test
    fun `no confirm when shared template text unchanged`() = runBlocking {
        val template = stubTemplate()
        val repo = object : StubRecordsRepository() {
            override suspend fun get(recordId: Long): Record? = stubRecord(1L, template)
            override suspend fun countRecordsForTemplate(templateId: Long): Int = 5
        }
        val result = ValidateEditRecordUseCase(repo).execute(
            recordId = 1L,
            title = "  ${template.name}  ",
            description = "  ${template.description}  ",
        )
        assertEquals(EditValidationResult.Valid, result)
    }

    @Test
    fun `returns error when title blank after trim`() = runBlocking {
        val template = stubTemplate()
        val repo = object : StubRecordsRepository() {
            override suspend fun get(recordId: Long): Record? = stubRecord(1L, template)
        }
        val result = ValidateEditRecordUseCase(repo).execute(1L, "   ", "Desc")
        assertTrue(result is EditValidationResult.Error)
        assertEquals("Title cannot be empty", (result as EditValidationResult.Error).message)
    }

    @Test
    fun `confirm when shared template and text changed`() = runBlocking {
        val template = stubTemplate(name = "Old", description = "D")
        val repo = object : StubRecordsRepository() {
            override suspend fun get(recordId: Long): Record? = stubRecord(1L, template)
            override suspend fun countRecordsForTemplate(templateId: Long): Int = 3
        }
        val result = ValidateEditRecordUseCase(repo).execute(1L, "New title", "D")
        assertTrue(result is EditValidationResult.ConfirmMultipleEdit)
        assertEquals(3, (result as EditValidationResult.ConfirmMultipleEdit).count)
    }

    @Test
    fun `valid when only one record uses template even if text changed`() = runBlocking {
        val template = stubTemplate(name = "Old", description = "D")
        val repo = object : StubRecordsRepository() {
            override suspend fun get(recordId: Long): Record? = stubRecord(1L, template)
            override suspend fun countRecordsForTemplate(templateId: Long): Int = 1
        }
        val result = ValidateEditRecordUseCase(repo).execute(1L, "New title", "D")
        assertEquals(EditValidationResult.Valid, result)
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
        override fun observeQuickPicks(count: Int) = emptyFlow<List<Template>>()
        override suspend fun getRecordsByTemplate(templateId: Long) = emptyList<Record>()
        override suspend fun countRecordsForTemplate(templateId: Long): Int = 0
        override fun observeRecords(searchTerm: String?, sinceEpochMillis: Long) = emptyFlow<List<Record>>()
        override suspend fun get(recordId: Long): Record? = null
        override fun observe(recordId: Long): Flow<Record> = emptyFlow()
        override suspend fun getTemplate(templateId: Long): Template = error("not used")
        override suspend fun saveTemplate(templateToSave: dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave) =
            0L
        override suspend fun saveRecord(templateId: Long, timestamp: ZonedDateTime) = 0L
        override suspend fun updateRecord(record: Record) = Unit
        override suspend fun deleteRecord(recordId: Long): Record = error("not used")
        override suspend fun deleteTemplateIfUnused(templateId: Long, imageToo: Boolean) = false to false
        override suspend fun updateTemplate(
            templateId: Long,
            name: String?,
            description: String?,
            templateImages: List<dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate>?,
            nutrients: Pair<TemplateNutrientBreakdown, TopContributors>?,
            notes: String?,
            mealComponents: List<dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent>?,
        ) = Unit
        override suspend fun addQuickPickOverride(templateId: Long, type: Template.QuickPickOverride) = Unit
        override suspend fun removeQuickPickOverride(templateId: Long) = Unit
    }
}
