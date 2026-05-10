package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.shared.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateRecordWithNewTemplateUseCaseTest {

    @Test
    fun `creates template then record from template`() = runBlocking {
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun saveTemplate(templateToSave: TemplateToSave) = 100L

            override suspend fun saveRecord(templateId: Long, timestamp: java.time.ZonedDateTime) = 555L
        }
        val createTemplate = CreateTemplateUseCase(repo)
        val createFromTpl = CreateRecordFromTemplateUseCase(repo)
        val id = CreateRecordWithNewTemplateUseCase(createTemplate, createFromTpl).execute(
            images = emptyList(),
            title = "Meal",
            description = "Note",
        )
        assertEquals(555L, id)
    }
}
