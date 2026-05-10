package dev.gaborbiro.dailymacros.features.modal.usecase

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetRecordImageUseCaseTest {

    @Test
    fun `null when template has no images`() = runBlocking {
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun get(recordId: Long) =
                ModalRecordFixtures.record(1L, ModalRecordFixtures.template(images = emptyList()))
        }
        assertNull(GetRecordImageUseCase(repo).execute(1L))
    }

    @Test
    fun `returns view dialog when images present`() = runBlocking {
        val repo = object : BaseRecordsRepositoryStub() {
            override suspend fun get(recordId: Long) = ModalRecordFixtures.record(
                1L,
                ModalRecordFixtures.template(
                    name = "Pizza",
                    images = listOf("a.jpg", "b.jpg"),
                ),
            )
        }
        val dlg = requireNotNull(GetRecordImageUseCase(repo).execute(1L))
        assertEquals("Pizza", dlg.title)
        assertEquals(listOf("a.jpg", "b.jpg"), dlg.images)
    }
}
