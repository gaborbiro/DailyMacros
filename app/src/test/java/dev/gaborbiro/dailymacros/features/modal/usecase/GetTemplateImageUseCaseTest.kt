package dev.gaborbiro.dailymacros.features.modal.usecase

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetTemplateImageUseCaseTest {

    @Test
    fun `null when template has no images`() = runBlocking {
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun getTemplate(templateId: Long) =
                ModalRecordFixtures.template(dbId = templateId, imageFilenames = emptyList())
        }
        assertNull(GetTemplateImageUseCase(repo).execute(9L))
    }

    @Test
    fun `returns view dialog when images present`() = runBlocking {
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun getTemplate(templateId: Long) = ModalRecordFixtures.template(
                dbId = templateId,
                name = "Salad",
                imageFilenames = listOf("x.png"),
            )
        }
        val dlg = requireNotNull(GetTemplateImageUseCase(repo).execute(9L))
        assertEquals("Salad", dlg.title)
        assertEquals(listOf("x.png"), dlg.imageFilenames)
    }
}
