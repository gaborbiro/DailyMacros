package dev.gaborbiro.dailymacros.repositories.chatgpt

import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.AiRequestError
import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import dev.gaborbiro.dailymacros.repositories.common.model.UsageLimitException
import dev.gaborbiro.dailymacros.repositories.common.model.UsageLimitKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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
    fun `upstream error maps to OperationFailed carrying it as cause, not a usage limit`() {
        val upstream = AiRequestError.Upstream(errorMessage = "server_error - boom")
        val result = mapper.map(upstream)

        assertTrue(result is DomainError.DisplayMessageToUser.OperationFailed)
        assertSame(upstream, (result as DomainError.DisplayMessageToUser.OperationFailed).cause)
    }

    @Test
    fun `network error maps to CheckInternetConnection`() {
        val result = mapper.map(AiRequestError.Network())

        assertTrue(result is DomainError.DisplayMessageToUser.CheckInternetConnection)
    }
}
