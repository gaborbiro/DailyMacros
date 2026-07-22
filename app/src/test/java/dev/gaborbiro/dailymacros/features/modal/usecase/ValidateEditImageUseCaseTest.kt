package dev.gaborbiro.dailymacros.features.modal.usecase

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateEditImageUseCaseTest {

    @Test
    fun `valid when single record uses template`() = runBlocking {
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun get(recordId: Long) =
                ModalRecordFixtures.record(1L, ModalRecordFixtures.template(dbId = 5L))

            override suspend fun countRecordsForTemplate(templateId: Long): Int = 1
        }
        val r = ValidateEditImageUseCase(repo).execute(1L)
        assertEquals(EditImageValidationResult.Valid, r)
    }

    @Test
    fun `asks confirmation when multiple records share template`() = runBlocking {
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun get(recordId: Long) =
                ModalRecordFixtures.record(1L, ModalRecordFixtures.template(dbId = 5L))

            override suspend fun countRecordsForTemplate(templateId: Long): Int = 4
        }
        val r = ValidateEditImageUseCase(repo).execute(1L)
        assertTrue(r is EditImageValidationResult.AskConfirmation)
        assertEquals(4, (r as EditImageValidationResult.AskConfirmation).count)
    }
}
