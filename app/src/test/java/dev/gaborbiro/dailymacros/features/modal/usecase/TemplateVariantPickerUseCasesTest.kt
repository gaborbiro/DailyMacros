package dev.gaborbiro.dailymacros.features.modal.usecase

import androidx.compose.ui.text.input.TextFieldValue
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.VariabilityArchetypePickerEntry
import dev.gaborbiro.dailymacros.repositories.records.TemplateVariabilityPreviewMapper
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilitySlotPreview
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityVariantPreview
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityArchetype
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityEvidence
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilitySlot
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityVariant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TemplateVariantPickerUseCasesTest {

    private val zone = ZoneId.of("UTC")
    private val previewMapper = TemplateVariabilityPreviewMapper()

    private fun stubTemplate(dbId: Long) = Template(
        dbId = dbId,
        images = emptyList(),
        isRepresentativeOfMealByImageIndex = emptyList(),
        name = "N",
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

    private fun archetypesForPicker(): List<VariabilityArchetype> = listOf(
        VariabilityArchetype(
            archetypeKey = "meal",
            displayName = "Meal",
            titleAliasesJson = "[]",
            evidenceCount = 2,
            lastSeenTimestamp = null,
            archetypeNotes = null,
            deprecated = false,
            deprecatedReason = null,
            slots = listOf(
                VariabilitySlot(
                    slotKey = "spread",
                    roleDisplayName = "Spread",
                    nutritionalLeversJson = "[]",
                    isHighVariability = true,
                    confidence = 1.0,
                    rationale = "",
                    variants = listOf(
                        VariabilityVariant(
                            variantKey = "butter",
                            variantLabel = "Butter",
                            notesExcerpt = "",
                            evidence = listOf(VariabilityEvidence(loggedAt = "a", templateId = 10L)),
                            sortOrder = 0,
                        ),
                        VariabilityVariant(
                            variantKey = "jam",
                            variantLabel = "Jam",
                            notesExcerpt = "",
                            evidence = listOf(VariabilityEvidence(loggedAt = "b", templateId = 20L)),
                            sortOrder = 1,
                        ),
                    ),
                    sortOrder = 0,
                ),
            ),
            sortOrder = 0,
        ),
    )

    private fun slotPreviews(): List<TemplateVariabilitySlotPreview> = listOf(
        TemplateVariabilitySlotPreview(
            archetypeKey = "meal",
            archetypeDisplayName = "Meal",
            slotKey = "spread",
            roleDisplayName = "Spread",
            variants = listOf(
                TemplateVariabilityVariantPreview("butter", "Butter"),
                TemplateVariabilityVariantPreview("jam", "Jam"),
            ),
        ),
    )

    private fun baseView(
        showLink: Boolean = true,
        entries: List<VariabilityArchetypePickerEntry> = listOf(
            VariabilityArchetypePickerEntry(
                archetypeKey = "meal",
                linkTitle = "Meal",
                slots = slotPreviews(),
            ),
        ),
        archetypes: List<VariabilityArchetype> = archetypesForPicker(),
    ) = DialogHandle.RecordDetailsDialog.View(
        recordId = 1L,
        templateDbId = 10L,
        nutrientBreakdown = null,
        allowEdit = true,
        titleHint = "",
        title = TextFieldValue(""),
        description = TextFieldValue(""),
        images = emptyList(),
        templateVariabilityPreview = null,
        variabilityArchetypes = archetypes,
        variabilityArchetypePickerEntries = entries,
        showVariabilityDifferentMealLink = showLink,
    )

    @Test
    fun `open use case skips when link hidden`() {
        val uc = OpenTemplateVariantPickerFromRecordDetailsUseCase(previewMapper)
        val r = uc.execute(baseView(showLink = false), archetypeKey = "meal")
        assertEquals(OpenTemplateVariantPickerResult.Skipped, r)
    }

    @Test
    fun `open use case returns ready picker`() {
        val uc = OpenTemplateVariantPickerFromRecordDetailsUseCase(previewMapper)
        val r = uc.execute(baseView(), archetypeKey = "meal") as OpenTemplateVariantPickerResult.Ready
        assertEquals(10L, r.picker.templateId)
        assertEquals("butter", r.picker.initialSlotSelections["spread"])
    }

    @Test
    fun `apply use case returns no op when combination maps to same template`() = runBlocking {
        val record = Record(1L, ZonedDateTime.now(zone), stubTemplate(10L))
        val repo = object : StubRecordsRepository() {
            override suspend fun get(recordId: Long) = record
        }
        val picker = (OpenTemplateVariantPickerFromRecordDetailsUseCase(previewMapper).execute(baseView(), "meal") as OpenTemplateVariantPickerResult.Ready).picker
        val result = ApplyTemplateVariantPickerSelectionUseCase(repo, previewMapper).execute(
            picker,
            mapOf("spread" to "butter"),
        )
        assertEquals(ApplyTemplateVariantPickerSelectionResult.NoOpSameTemplate, result)
    }

    @Test
    fun `apply use case updates record when combination maps to different template`() = runBlocking {
        val record = Record(1L, ZonedDateTime.now(zone), stubTemplate(10L))
        var updated: Record? = null
        val repo = object : StubRecordsRepository() {
            override suspend fun get(recordId: Long) = record
            override suspend fun getTemplate(templateId: Long) = stubTemplate(20L)
            override suspend fun updateRecord(record: Record) {
                updated = record
            }
        }
        val picker = (OpenTemplateVariantPickerFromRecordDetailsUseCase(previewMapper).execute(baseView(), "meal") as OpenTemplateVariantPickerResult.Ready).picker
        val result = ApplyTemplateVariantPickerSelectionUseCase(repo, previewMapper).execute(
            picker,
            mapOf("spread" to "jam"),
        )
        assertEquals(ApplyTemplateVariantPickerSelectionResult.Applied, result)
        assertEquals(20L, updated?.template?.dbId)
    }

    @Test
    fun `apply use case record not found`() = runBlocking {
        val repo = object : StubRecordsRepository() {
            override suspend fun get(recordId: Long) = null
        }
        val picker = (OpenTemplateVariantPickerFromRecordDetailsUseCase(previewMapper).execute(baseView(), "meal") as OpenTemplateVariantPickerResult.Ready).picker
        val result = ApplyTemplateVariantPickerSelectionUseCase(repo, previewMapper).execute(
            picker,
            mapOf("spread" to "jam"),
        )
        assertEquals(ApplyTemplateVariantPickerSelectionResult.RecordNotFound, result)
    }

    @Test
    fun `apply use case unknown combination`() = runBlocking {
        val record = Record(1L, ZonedDateTime.now(zone), stubTemplate(10L))
        val repo = object : StubRecordsRepository() {
            override suspend fun get(recordId: Long) = record
        }
        val picker = (OpenTemplateVariantPickerFromRecordDetailsUseCase(previewMapper).execute(baseView(), "meal") as OpenTemplateVariantPickerResult.Ready).picker
        val result = ApplyTemplateVariantPickerSelectionUseCase(repo, previewMapper).execute(
            picker,
            mapOf("spread" to "nonexistent"),
        )
        assertEquals(ApplyTemplateVariantPickerSelectionResult.UnknownCombination, result)
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
        override suspend fun getTemplate(templateId: Long): Template = error("override")
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
}
