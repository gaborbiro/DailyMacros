package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.ChatGPTDomainError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MacrosNotificationTextMapperTest {

    private val mapper = MacrosNotificationTextMapper()

    @Test
    fun `mapDomainErrorToUserMessage check internet`() {
        assertEquals(
            "Internet connectivity error",
            mapper.mapDomainErrorToUserMessage(ChatGPTDomainError.DisplayMessageToUser.CheckInternetConnection()),
        )
    }

    @Test
    fun `mapDomainErrorToUserMessage contact support`() {
        assertTrue(
            mapper.mapDomainErrorToUserMessage(ChatGPTDomainError.DisplayMessageToUser.ContactSupport())
                .contains("engineers"),
        )
    }

    @Test
    fun `mapDomainErrorToUserMessage custom message`() {
        assertEquals(
            "Hello",
            mapper.mapDomainErrorToUserMessage(ChatGPTDomainError.DisplayMessageToUser.Message("Hello")),
        )
    }

    @Test
    fun `mapDomainErrorToUserMessage try again`() {
        assertTrue(
            mapper.mapDomainErrorToUserMessage(ChatGPTDomainError.DisplayMessageToUser.TryAgain())
                .contains("try again"),
        )
    }
}
