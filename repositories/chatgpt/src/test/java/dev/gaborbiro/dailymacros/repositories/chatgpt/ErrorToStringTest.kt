package dev.gaborbiro.dailymacros.repositories.chatgpt

import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.AiRequestError
import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.PrintWriter
import java.io.StringWriter

class ErrorToStringTest {

    private val body = "<html><title>500 Server Error</title></html>"
    private val upstreamMessage = "HTTP 500 - unable to discern structured error"
    private val upstream = AiRequestError.Upstream(errorMessage = upstreamMessage, cause = Exception(body))
    private val domainError = DomainError.DisplayMessageToUser.OperationFailed(cause = upstream)

    @Test
    fun `toString is class name and message only`() {
        assertEquals(
            "${AiRequestError.Upstream::class.java.name}: $upstreamMessage",
            upstream.toString(),
        )
        assertEquals(
            DomainError.DisplayMessageToUser.OperationFailed::class.java.name,
            domainError.toString(),
        )
    }

    @Test
    fun `stack trace prints each message once`() {
        val trace = StringWriter().also { domainError.printStackTrace(PrintWriter(it)) }.toString()

        assertEquals(1, trace.split(upstreamMessage).size - 1)
        assertEquals(1, trace.split(body).size - 1)
    }
}
