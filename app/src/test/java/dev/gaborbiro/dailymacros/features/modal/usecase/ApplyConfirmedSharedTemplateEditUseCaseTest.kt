package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import android.app.Application
import dev.gaborbiro.dailymacros.test.WorkManagerTestSupport
import dev.gaborbiro.dailymacros.features.modal.model.ChangeImagesTarget
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateImageUpdate
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class ApplyConfirmedSharedTemplateEditUseCaseTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestSupport.setUp(context)
    }

    @After
    fun tearDown() {
        WorkManagerTestSupport.tearDown(context)
    }

    @Test
    fun `record target forks template and updates record`() = runBlocking {
        val base = ModalRecordFixtures.template(dbId = 10L)
        var updated: Record? = null
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun get(recordId: Long) = ModalRecordFixtures.record(1L, base)

            override suspend fun saveTemplate(templateToSave: TemplateToSave) = 88L

            override suspend fun getTemplate(templateId: Long) = ModalRecordFixtures.template(dbId = 88L, name = "Forked")

            override suspend fun updateRecord(record: Record) {
                updated = record
            }
        }
        val createTemplate = CreateTemplateUseCase(repo)
        val updateRec = UpdateRecordWithNewTemplateUseCase(repo, createTemplate)
        val uc = ApplyConfirmedSharedTemplateEditUseCase(updateRec, repo, context)
        uc.execute(
            target = ChangeImagesTarget.RECORD,
            recordId = 1L,
            images = listOf("a.jpg"),
            title = "T",
            description = "D",
        )
        assertEquals(88L, updated!!.template.dbId)
    }

    @Test
    fun `template target updates shared template`() = runBlocking {
        var templateIdSeen: Long? = null
        var nameSeen: String? = null
        var imagesSeen: List<TemplateImageUpdate>? = null
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun get(recordId: Long) =
                ModalRecordFixtures.record(1L, ModalRecordFixtures.template(dbId = 33L))

            override suspend fun updateTemplate(
                templateId: Long,
                name: String?,
                description: String?,
                templateImages: List<TemplateImageUpdate>?,
                nutrients: Pair<Nutrients, dev.gaborbiro.dailymacros.repositories.common.model.TopContributors>?,
                notes: String?,
                mealComponents: List<dev.gaborbiro.dailymacros.repositories.records.domain.model.MealComponent>?,
            ) {
                templateIdSeen = templateId
                nameSeen = name
                imagesSeen = templateImages
            }
        }
        val createTemplate = CreateTemplateUseCase(repo)
        val updateRec = UpdateRecordWithNewTemplateUseCase(repo, createTemplate)
        val uc = ApplyConfirmedSharedTemplateEditUseCase(updateRec, repo, context)
        uc.execute(
            target = ChangeImagesTarget.TEMPLATE,
            recordId = 1L,
            images = listOf("b.jpg"),
            title = "Shared",
            description = "X",
        )
        assertEquals(33L, templateIdSeen)
        assertEquals("Shared", nameSeen)
        assertEquals(listOf(TemplateImageUpdate(filename = "b.jpg")), imagesSeen)
    }
}
