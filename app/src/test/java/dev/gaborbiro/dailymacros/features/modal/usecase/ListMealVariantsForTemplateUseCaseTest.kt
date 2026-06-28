package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.shared.ListMealVariantsForTemplateUseCase
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ListMealVariantsForTemplateUseCaseTest {

    private val zone = ZoneId.of("UTC")

    private fun stubTemplate(dbId: Long, name: String, parentId: Long? = null) = Template(
        dbId = dbId,
        images = emptyList(),
        isRepresentativeOfMealByImageIndex = emptyList(),
        name = name,
        description = "",
        parentTemplateId = parentId,
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
    fun `hasOtherVariants false when only template in family`() = runBlocking {
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long) = listOf(1L)
        }
        assertFalse(ListMealVariantsForTemplateUseCase(repo).hasOtherVariants(1L))
    }

    @Test
    fun `hasOtherVariants false when siblings exist but none have records`() = runBlocking {
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long) = listOf(1L, 2L)
        }
        assertFalse(ListMealVariantsForTemplateUseCase(repo).hasOtherVariants(1L))
    }

    @Test
    fun `hasOtherVariants true when another family member has records`() = runBlocking {
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long) = listOf(1L, 2L)

            override suspend fun getRecordsByTemplate(templateId: Long) = when (templateId) {
                2L -> listOf(
                    Record(
                        recordId = 1L,
                        timestamp = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, zone),
                        template = stubTemplate(2L, "B"),
                    ),
                )
                else -> emptyList()
            }

            override suspend fun countRecordsForTemplate(templateId: Long): Int =
                getRecordsByTemplate(templateId).size
        }
        assertTrue(ListMealVariantsForTemplateUseCase(repo).hasOtherVariants(1L))
    }

    @Test
    fun `execute lists siblings sorted by lastUsed`() = runBlocking {
        val t1 = stubTemplate(1L, "Root")
        val t2 = stubTemplate(2L, "Child", parentId = 1L)
        val r2Old = Record(
            recordId = 10L,
            timestamp = ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, zone),
            template = t2,
        )
        val r2New = Record(
            recordId = 11L,
            timestamp = ZonedDateTime.of(2024, 6, 1, 12, 0, 0, 0, zone),
            template = t2,
        )
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long) = listOf(1L, 2L, 3L)
            override suspend fun getTemplate(templateId: Long): Template = when (templateId) {
                1L -> t1
                2L -> t2
                3L -> stubTemplate(3L, "Unused sibling")
                else -> error("unknown $templateId")
            }

            override suspend fun getRecordsByTemplate(templateId: Long): List<Record> = when (templateId) {
                2L -> listOf(r2Old, r2New)
                else -> emptyList()
            }

            override suspend fun countRecordsForTemplate(templateId: Long): Int =
                getRecordsByTemplate(templateId).size
        }
        val result = ListMealVariantsForTemplateUseCase(repo).execute(1L)
        assertNotNull(result)
        assertEquals(1L, result!!.current.templateId)
        assertEquals(1, result.others.size)
        assertEquals(listOf(2L), result.others.map { it.templateId })
    }
}
