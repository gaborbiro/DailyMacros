package dev.gaborbiro.dailymacros.features.overview.usecase

import dev.gaborbiro.dailymacros.features.modal.usecase.BaseRecordsRepositoryStub
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteUnusedTemplateIfOrphanedUseCaseTest {

    @Test
    fun `execute delegates to repository`() = runBlocking {
        val stub = object : BaseRecordsRepositoryStub() {
            var capturedId: Long? = null
            override suspend fun deleteTemplateIfUnused(
                templateId: Long,
                imageToo: Boolean,
            ): Pair<Boolean, Boolean> {
                capturedId = templateId
                return true to false
            }
        }
        DeleteUnusedTemplateIfOrphanedUseCase(stub).execute(77L)
        assertEquals(77L, stub.capturedId)
    }
}
