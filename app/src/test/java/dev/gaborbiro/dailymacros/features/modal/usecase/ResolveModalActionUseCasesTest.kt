package dev.gaborbiro.dailymacros.features.modal.usecase

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveModalActionUseCasesTest {

    @Test
    fun `resolve select record uses template name`() = runBlocking {
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun get(recordId: Long) =
                ModalRecordFixtures.record(5L, ModalRecordFixtures.template(name = "Soup"))
        }
        val dlg = ResolveSelectRecordActionDialogUseCase(repo).execute(5L)
        assertEquals(5L, dlg.recordId)
        assertEquals("Soup", dlg.title)
    }

    @Test
    fun `resolve select record empty title when missing`() = runBlocking {
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun get(recordId: Long) = null
        }
        val dlg = ResolveSelectRecordActionDialogUseCase(repo).execute(5L)
        assertEquals("", dlg.title)
    }

    @Test
    fun `resolve select template uses first record template name`() = runBlocking {
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun getRecordsByTemplate(templateId: Long) = listOf(
                ModalRecordFixtures.record(1L, ModalRecordFixtures.template(name = "Bread")),
            )
        }
        val dlg = ResolveSelectTemplateActionDialogUseCase(repo).execute(8L)
        assertEquals(8L, dlg.templateId)
        assertEquals("Bread", dlg.title)
    }

    @Test
    fun `resolve select template empty when no records`() = runBlocking {
        val dlg = ResolveSelectTemplateActionDialogUseCase(BaseRecordsRepositoryStub()).execute(8L)
        assertEquals("", dlg.title)
    }

    @Test
    fun `resolve first record id`() = runBlocking {
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun getRecordsByTemplate(templateId: Long) = listOf(
                ModalRecordFixtures.record(42L, ModalRecordFixtures.template()),
            )
        }
        assertEquals(42L, ResolveFirstRecordIdForTemplateUseCase(repo).execute(1L))
    }

    @Test
    fun `resolve first record id null`() = runBlocking {
        assertNull(ResolveFirstRecordIdForTemplateUseCase(BaseRecordsRepositoryStub()).execute(1L))
    }
}
