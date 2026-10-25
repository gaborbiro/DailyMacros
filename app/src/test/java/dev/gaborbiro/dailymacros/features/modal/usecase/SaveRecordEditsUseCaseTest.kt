package dev.gaborbiro.dailymacros.features.modal.usecase

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import dev.gaborbiro.dailymacros.features.modal.model.RecordDetailsPristineSnapshot
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.common.model.TopContributors
import dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.test.WorkManagerTestSupport
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class SaveRecordEditsUseCaseTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = WorkManagerTestSupport.setUp(app)

    @After
    fun tearDown() = WorkManagerTestSupport.tearDown(app)

    private fun analysisQueuedFor(recordId: Long): Boolean =
        WorkManager.getInstance(app).getWorkInfosForUniqueWork("nutrient_analysis_$recordId").get()
            .any { !it.state.isFinished }

    /** Record 5 on template 7, the only logged variant of its meal unless [familyRecordCounts] says otherwise. */
    private class Repo(
        private val familyRecordCounts: Map<Long, Int> = mapOf(7L to 1),
    ) : BaseRecordsRepositoryStub() {
        val record = ModalRecordFixtures.record(5L, ModalRecordFixtures.template(dbId = 7L, name = "Soup"))
        val savedTemplates = mutableListOf<TemplateToSave>()
        val updatedTemplateIds = mutableListOf<Long>()

        override suspend fun get(recordId: Long): Record? = record.takeIf { it.recordId == recordId }
        override suspend fun getTemplateIdsInSameVariantFamily(templateId: Long) = familyRecordCounts.keys.toList()
        override suspend fun countRecordsForTemplate(templateId: Long) = familyRecordCounts[templateId] ?: 0
        override suspend fun getRecordsByTemplate(templateId: Long) = if (templateId == 7L) listOf(record) else emptyList()
        override suspend fun getTemplate(templateId: Long) = ModalRecordFixtures.template(dbId = templateId)
        override suspend fun saveTemplate(templateToSave: TemplateToSave): Long {
            savedTemplates += templateToSave
            return 42L
        }

        override suspend fun updateTemplate(
            templateId: Long,
            name: String?,
            description: String?,
            templateImages: List<TemplateImageUpdate>?,
            nutrients: Pair<Nutrients, TopContributors>?,
            notes: String?,
            mealComponents: List<MealComponent>?,
        ) {
            updatedTemplateIds += templateId
        }
    }

    private val pristine = RecordDetailsPristineSnapshot(
        templateDbId = 7L,
        title = "Soup",
        description = "Lentil",
        imageFilenames = listOf("a.jpg", "b.jpg"),
    )

    private suspend fun Repo.save(
        title: String = "Soup",
        imageFilenames: List<String> = listOf("a.jpg", "b.jpg"),
        knownToHaveLoggedVariants: Boolean = false,
    ) {
        val createTemplate = CreateTemplateUseCase(this)
        SaveRecordEditsUseCase(app, this, UpdateRecordWithNewTemplateUseCase(this, createTemplate)).execute(
            recordId = 5L,
            templateId = 7L,
            pristine = pristine,
            knownToHaveLoggedVariants = knownToHaveLoggedVariants,
            title = title,
            description = "Lentil",
            imageFilenames = imageFilenames,
            imageRepresentativeFlags = emptyMap(),
        )
    }

    @Test
    fun `new wording on the only variant forks the template and re-analyzes`() = runTest {
        val repo = Repo()
        repo.save(title = "Stew")
        assertEquals("Stew", repo.savedTemplates.single().name)
        assertTrue(repo.updatedTemplateIds.isEmpty())
        assertTrue(analysisQueuedFor(5L))
    }

    @Test
    fun `reordered photos on the only variant update the template in place`() = runTest {
        val repo = Repo()
        repo.save(imageFilenames = listOf("b.jpg", "a.jpg"))
        assertEquals(listOf(7L), repo.updatedTemplateIds)
        assertTrue(repo.savedTemplates.isEmpty())
        assertFalse(analysisQueuedFor(5L))
    }

    @Test
    fun `unchanged wording and photos on the only variant update the template in place`() = runTest {
        val repo = Repo()
        repo.save()
        assertEquals(listOf(7L), repo.updatedTemplateIds)
        assertTrue(repo.savedTemplates.isEmpty())
        assertFalse(analysisQueuedFor(5L))
    }

    @Test
    fun `new wording on a meal with logged variants updates the shared template and re-analyzes`() = runTest {
        val repo = Repo(familyRecordCounts = mapOf(7L to 1, 8L to 2))
        repo.save(title = "Stew")
        assertEquals(listOf(7L), repo.updatedTemplateIds)
        assertTrue(repo.savedTemplates.isEmpty())
        assertTrue(analysisQueuedFor(5L))
    }

    @Test
    fun `a variant without logs doesn't make the meal varied`() = runTest {
        val repo = Repo(familyRecordCounts = mapOf(7L to 1, 8L to 0))
        repo.save(title = "Stew")
        assertTrue(repo.updatedTemplateIds.isEmpty())
        assertEquals(1, repo.savedTemplates.size)
    }

    @Test
    fun `known logged variants skip the lookup and update the shared template`() = runTest {
        val repo = Repo()
        repo.save(title = "Stew", knownToHaveLoggedVariants = true)
        assertEquals(listOf(7L), repo.updatedTemplateIds)
        assertTrue(repo.savedTemplates.isEmpty())
        assertTrue(analysisQueuedFor(5L))
    }
}
