package dev.gaborbiro.dailymacros.repositories.chatgpt

import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.AiRequestError
import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import dev.gaborbiro.dailymacros.repositories.common.model.UsageLimitException
import dev.gaborbiro.dailymacros.repositories.common.model.UsageLimitKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatGPTMapperTest {

    private val mapper = ChatGPTMapper()

    @Test
    fun `proxy usage limit maps to OperationFailed carrying the kind as cause`() {
        UsageLimitKind.values().forEach { kind ->
            val result = mapper.map(AiRequestError.Proxy(kind))

            assertTrue(result is DomainError.DisplayMessageToUser.OperationFailed)
            val cause = (result as DomainError.DisplayMessageToUser.OperationFailed).cause
            assertTrue(cause is UsageLimitException)
            assertEquals(kind, (cause as UsageLimitException).kind)
        }
    }

    @Test
    fun `upstream error maps to power-user TechnicalMessage, not a usage limit`() {
        val result = mapper.map(AiRequestError.Upstream(errorMessage = "server_error - boom"))

        assertTrue(result is DomainError.DisplayMessageToUser.TechnicalMessage)
        assertEquals(
            "server_error - boom",
            (result as DomainError.DisplayMessageToUser.TechnicalMessage).errorMessage,
        )
    }

    @Test
    fun `network error maps to CheckInternetConnection`() {
        val result = mapper.map(AiRequestError.Network())

        assertTrue(result is DomainError.DisplayMessageToUser.CheckInternetConnection)
    }
}
