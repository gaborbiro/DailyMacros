package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateTemplateUseCaseTest {

    @Test
    fun `persists template and returns id`() = runBlocking {
        var saved: TemplateToSave? = null
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun saveTemplate(templateToSave: TemplateToSave): Long {
                saved = templateToSave
                return 77L
            }
        }
        val id = CreateTemplateUseCase(repo).execute(
            images = listOf("a.jpg"),
            title = "T",
            description = "D",
            parentTemplateId = 3L,
        )
        assertEquals(77L, id)
        val captured = requireNotNull(saved)
        assertEquals("T", captured.name)
        assertEquals("D", captured.description)
        assertEquals(listOf("a.jpg"), captured.images)
        assertEquals(3L, captured.parentTemplateId)
    }
}
