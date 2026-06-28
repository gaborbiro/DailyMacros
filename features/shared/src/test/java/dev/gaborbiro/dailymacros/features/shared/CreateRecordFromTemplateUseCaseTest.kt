package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.common.model.TopContributors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class CreateRecordFromTemplateUseCaseTest {

    @Test
    fun `execute saves record with current timestamp and returns new id`() = runBlocking {
        var savedTemplateId: Long? = null
        var savedAt: ZonedDateTime? = null
        val repo = object : StubRecordsRepository() {
            override suspend fun saveRecord(templateId: Long, timestamp: ZonedDateTime): Long {
                savedTemplateId = templateId
                savedAt = timestamp
                return 42L
            }
        }
        val before = ZonedDateTime.now(ZoneId.systemDefault())
        val id = CreateRecordFromTemplateUseCase(repo).execute(7L)
        val after = ZonedDateTime.now(ZoneId.systemDefault())
        assertEquals(42L, id)
        assertEquals(7L, savedTemplateId)
        val ts = requireNotNull(savedAt)
        val delta = Duration.between(before, ts).abs()
        assertTrue(delta.toMillis() < 5000)
        assertTrue(!ts.isBefore(before) && !ts.isAfter(after))
    }

    private open class StubRecordsRepository : RecordsRepository {
        override suspend fun getRecords(since: ZonedDateTime?) = emptyList<Record>()
        override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long) = emptyList<Long>()
        override suspend fun countTemplates(): Int = 0
        override fun getMostRecentRecord(): Record? = null
        override suspend fun getQuickPicks(count: Int) = emptyList<Template>()
        override fun observeQuickPicks(count: Int) = emptyFlow<List<Template>>()
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
            nutrients: Pair<Nutrients, TopContributors>?,
            notes: String?,
            mealComponents: List<MealComponent>?,
        ) = Unit
        override suspend fun addQuickPickOverride(templateId: Long, type: Template.QuickPickOverride) = Unit
        override suspend fun removeQuickPickOverride(templateId: Long) = Unit
    }
}
