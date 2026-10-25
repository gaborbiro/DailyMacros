package dev.gaborbiro.dailymacros.features.modal.usecase

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.test.WorkManagerTestSupport
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class ScheduleAnalysisIfTemplateIncompleteUseCaseTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = WorkManagerTestSupport.setUp(app)

    @After
    fun tearDown() = WorkManagerTestSupport.tearDown(app)

    private fun analysisQueuedFor(recordId: Long): Boolean =
        WorkManager.getInstance(app).getWorkInfosForUniqueWork("nutrient_analysis_$recordId").get()
            .any { !it.state.isFinished }

    private fun useCase(template: Template) = ScheduleAnalysisIfTemplateIncompleteUseCase(
        appContext = app,
        recordsRepository = object : BaseRecordsRepositoryStub() {
            override suspend fun getTemplate(templateId: Long) = template
        },
    )

    @Test
    fun `queues analysis for a pending meal`() = runTest {
        useCase(ModalRecordFixtures.template(dbId = 7L).copy(isPending = true, nutrients = Nutrients(calories = 300)))
            .execute(recordId = 5L, templateId = 7L)
        assertTrue(analysisQueuedFor(5L))
    }

    @Test
    fun `queues analysis for a meal without calories`() = runTest {
        useCase(ModalRecordFixtures.template(dbId = 7L).copy(nutrients = Nutrients(protein = 10f)))
            .execute(recordId = 5L, templateId = 7L)
        assertTrue(analysisQueuedFor(5L))
    }

    @Test
    fun `leaves an analysed meal alone`() = runTest {
        useCase(ModalRecordFixtures.template(dbId = 7L).copy(nutrients = Nutrients(calories = 300)))
            .execute(recordId = 5L, templateId = 7L)
        assertFalse(analysisQueuedFor(5L))
    }
}
