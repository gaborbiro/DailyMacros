package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import android.app.Application
import dev.gaborbiro.dailymacros.test.WorkManagerTestSupport
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
class DeleteRecordUseCaseTest {

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
    fun `deletes record then unused template cleanup`() = runBlocking {
        var deletedRecordId: Long? = null
        var unusedTemplateId: Long? = null
        val deletedRecord = ModalRecordFixtures.record(1L, ModalRecordFixtures.template(dbId = 7L))
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun deleteRecord(recordId: Long): dev.gaborbiro.dailymacros.repositories.records.domain.model.Record {
                deletedRecordId = recordId
                return deletedRecord
            }

            override suspend fun deleteTemplateIfUnused(templateId: Long, imageToo: Boolean): Pair<Boolean, Boolean> {
                unusedTemplateId = templateId
                return true to false
            }
        }
        DeleteRecordUseCase(repo, context).execute(1L)
        assertEquals(1L, deletedRecordId)
        assertEquals(7L, unusedTemplateId)
    }
}
