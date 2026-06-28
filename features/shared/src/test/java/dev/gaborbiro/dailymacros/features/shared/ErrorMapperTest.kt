package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorMapperTest {

    private val mapper = ErrorMapper()

    @Test
    fun `mapErrorMessage check internet`() {
        assertEquals(
            "Internet connectivity error",
            mapper.mapErrorMessage(DomainError.DisplayMessageToUser.CheckInternetConnection()),
        )
    }

    @Test
    fun `mapErrorMessage contact support`() {
        assertTrue(
            mapper.mapErrorMessage(DomainError.DisplayMessageToUser.ContactSupport())
                .contains("engineers"),
        )
    }

    @Test
    fun `mapErrorMessage custom message`() {
        assertEquals(
            "Hello",
            mapper.mapErrorMessage(DomainError.DisplayMessageToUser.Message("Hello")),
        )
    }

    @Test
    fun `mapErrorMessage try again`() {
        assertTrue(
            mapper.mapErrorMessage(DomainError.DisplayMessageToUser.TryAgain())
                .contains("try again"),
        )
    }
}
