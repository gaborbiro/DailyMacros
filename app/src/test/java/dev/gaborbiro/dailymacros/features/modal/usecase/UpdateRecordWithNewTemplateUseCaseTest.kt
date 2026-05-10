package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateToSave
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateRecordWithNewTemplateUseCaseTest {

    @Test
    fun `replaces record template with newly saved template`() = runBlocking {
        val oldTpl = ModalRecordFixtures.template(dbId = 10L)
        val newTpl = ModalRecordFixtures.template(dbId = 99L, name = "New")
        var updatedRecord: dev.gaborbiro.dailymacros.repositories.records.domain.model.Record? = null
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun get(recordId: Long) =
                ModalRecordFixtures.record(1L, oldTpl)

            override suspend fun saveTemplate(templateToSave: TemplateToSave) = 99L

            override suspend fun getTemplate(templateId: Long) = newTpl

            override suspend fun updateRecord(record: dev.gaborbiro.dailymacros.repositories.records.domain.model.Record) {
                updatedRecord = record
            }
        }
        UpdateRecordWithNewTemplateUseCase(repo, CreateTemplateUseCase(repo)).execute(
            recordId = 1L,
            images = listOf("z.jpg"),
            title = "New",
            description = "D",
        )
        val rec = requireNotNull(updatedRecord)
        assertEquals(99L, rec.template.dbId)
        assertEquals("New", rec.template.name)
    }
}
