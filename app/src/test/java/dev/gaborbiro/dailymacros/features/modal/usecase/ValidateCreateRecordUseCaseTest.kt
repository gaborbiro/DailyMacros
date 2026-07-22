package dev.gaborbiro.dailymacros.features.modal.usecase

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateCreateRecordUseCaseTest {

    @Test
    fun `returns valid for any input`() = runBlocking {
        val result = ValidateCreateRecordUseCase().execute(
            imageFilenames = emptyList(),
            title = "",
            description = "",
        )
        assertEquals(CreateValidationResult.Valid, result)
    }
}
