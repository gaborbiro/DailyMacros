package dev.gaborbiro.dailymacros.features.settings.variability

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.FoodRecognitionResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisRequest
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.VariabilityMiningResult
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.VariabilityRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.MealVariabilityProfileSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class MineMealVariabilityPreviewUseCaseTest {

    private val zone = ZoneId.of("UTC")

    private fun stubTemplate(
        dbId: Long = 1L,
        createdAtEpochMs: Long = 0L,
        updatedAtEpochMs: Long = 0L,
    ) = Template(
        dbId = dbId,
        images = emptyList(),
        isRepresentativeOfMealByImageIndex = emptyList(),
        name = "N",
        description = "D",
        parentTemplateId = null,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs,
        isPending = false,
        nutrients = TemplateNutrientBreakdown(),
        notes = "",
        mealComponents = emptyList(),
        topContributors = TopContributors(),
        quickPickOverride = null,
    )

    private fun stubRecord(
        recordId: Long,
        template: Template,
    ) = Record(
        recordId = recordId,
        timestamp = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, zone),
        template = template,
    )

    @Test
    fun `no snapshot uses recent records and persists ingest watermark from template activity`() = runBlocking {
        val template = stubTemplate(dbId = 1L, createdAtEpochMs = 500L, updatedAtEpochMs = 100L)
        val recordsRepo = object : StubRecordsRepository() {
            override suspend fun getRecentRecords(limit: Int) =
                listOf(stubRecord(1L, template))
        }
        val variability = StubVariabilityRepository(latest = null)
        val chat = StubChatGptRepository()
        val preview = MineMealVariabilityPreviewUseCase(recordsRepo, chat, variability).execute()
        assertTrue(!preview.skippedNoNewObservations)
        assertEquals(1, chat.mineCalls)
        assertEquals(1, variability.replaceCalls.size)
        assertEquals(500L, variability.replaceCalls.single().templatesIngestWatermarkEpochMs)
    }

    @Test
    fun `snapshot with watermark zero uses full recent window not delta query`() = runBlocking {
        val snap = MealVariabilityProfileSnapshot(
            minedAtEpochMs = 1L,
            profileJson = """{"archetypes":[]}""",
            templatesIngestWatermarkEpochMs = 0L,
        )
        var deltaCalls = 0
        var recentCalls = 0
        val recordsRepo = object : StubRecordsRepository() {
            override suspend fun getRecentRecords(limit: Int): List<Record> {
                recentCalls++
                return listOf(stubRecord(1L, stubTemplate(createdAtEpochMs = 10L)))
            }

            override suspend fun getRecordsForVariabilityDelta(limit: Int, afterWatermarkExclusive: Long): List<Record> {
                deltaCalls++
                return emptyList()
            }
        }
        val variability = StubVariabilityRepository(latest = snap)
        val chat = StubChatGptRepository()
        MineMealVariabilityPreviewUseCase(recordsRepo, chat, variability).execute()
        assertEquals(1, recentCalls)
        assertEquals(0, deltaCalls)
        assertEquals(1, chat.mineCalls)
    }

    @Test
    fun `incremental mode with empty delta skips model and persistence`() = runBlocking {
        val snap = MealVariabilityProfileSnapshot(
            minedAtEpochMs = 1L,
            profileJson = """{"archetypes":[]}""",
            templatesIngestWatermarkEpochMs = 200L,
        )
        val recordsRepo = object : StubRecordsRepository() {
            override suspend fun getRecordsForVariabilityDelta(limit: Int, afterWatermarkExclusive: Long): List<Record> {
                assertEquals(200L, afterWatermarkExclusive)
                return emptyList()
            }
        }
        val variability = StubVariabilityRepository(latest = snap)
        val chat = StubChatGptRepository()
        val preview = MineMealVariabilityPreviewUseCase(recordsRepo, chat, variability).execute()
        assertTrue(preview.skippedNoNewObservations)
        assertEquals(0, chat.mineCalls)
        assertTrue(variability.replaceCalls.isEmpty())
        assertTrue(preview.requestJsonPretty.contains("no_meal_observations_after_watermark"))
    }

    @Test
    fun `ingest watermark uses minedAt when template timestamps are zero`() = runBlocking {
        val snap = MealVariabilityProfileSnapshot(
            minedAtEpochMs = 1L,
            profileJson = """{"archetypes":[]}""",
            templatesIngestWatermarkEpochMs = 50L,
        )
        val template = stubTemplate(dbId = 1L, createdAtEpochMs = 0L, updatedAtEpochMs = 0L)
        val recordsRepo = object : StubRecordsRepository() {
            override suspend fun getRecordsForVariabilityDelta(limit: Int, afterWatermarkExclusive: Long) =
                listOf(stubRecord(1L, template))
        }
        val variability = StubVariabilityRepository(latest = snap)
        val chat = StubChatGptRepository()
        MineMealVariabilityPreviewUseCase(recordsRepo, chat, variability).execute()
        val w = variability.replaceCalls.single().templatesIngestWatermarkEpochMs
        assertTrue("expected watermark >= 50 and mined-at fallback, got $w", w >= 50L)
    }

    private open class StubRecordsRepository : RecordsRepository {
        override suspend fun getRecords(since: java.time.ZonedDateTime?) = emptyList<Record>()
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
        override suspend fun saveRecord(templateId: Long, timestamp: java.time.ZonedDateTime) = 0L
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

    private class StubVariabilityRepository(
        private val latest: MealVariabilityProfileSnapshot?,
    ) : VariabilityRepository {
        val replaceCalls = mutableListOf<ReplaceArgs>()

        override suspend fun getLatestProfile(): MealVariabilityProfileSnapshot? = latest

        override suspend fun replaceProfile(profile: dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.MealVariabilityPersistedProfile) =
            Unit

        override suspend fun replaceProfileFromModelJson(
            profileJson: String,
            minedAtEpochMs: Long,
            templatesIngestWatermarkEpochMs: Long,
        ) {
            replaceCalls += ReplaceArgs(profileJson, minedAtEpochMs, templatesIngestWatermarkEpochMs)
        }

        override suspend fun clearProfile() = Unit

        data class ReplaceArgs(
            val profileJson: String,
            val minedAtEpochMs: Long,
            val templatesIngestWatermarkEpochMs: Long,
        )
    }

    private class StubChatGptRepository : ChatGPTRepository {
        var mineCalls = 0

        override suspend fun recogniseFood(request: FoodRecognitionRequest): FoodRecognitionResult = error("not used")

        override suspend fun analyseNutrients(request: NutrientAnalysisRequest): NutrientAnalysisResult =
            error("not used")

        override suspend fun mineMealVariability(userMessageJson: String): VariabilityMiningResult {
            mineCalls++
            return VariabilityMiningResult(profileJson = """{"archetypes":[]}""")
        }
    }
}
