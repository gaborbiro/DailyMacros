package dev.gaborbiro.dailymacros.repositories.chatgpt

import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.AiRequestError
import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.PrintWriter
import java.io.StringWriter

class ErrorToStringTest {

    private val body = "<html><title>500 Server Error</title></html>"
    private val upstream = AiRequestError.Upstream(errorMessage = "500 - unable to discern error message", cause = Exception(body))
    private val domainError = DomainError.DisplayMessageToUser.TechnicalMessage(errorMessage = "500 - unable to discern error message", cause = upstream)

    @Test
    fun `toString is class name and message only`() {
        assertEquals(
            "${AiRequestError.Upstream::class.java.name}: 500 - unable to discern error message",
            upstream.toString(),
        )
        assertEquals(
            "${DomainError.DisplayMessageToUser.TechnicalMessage::class.java.name}: 500 - unable to discern error message",
            domainError.toString(),
        )
    }

    @Test
    fun `stack trace prints the root cause message once`() {
        val trace = StringWriter().also { domainError.printStackTrace(PrintWriter(it)) }.toString()

        assertEquals(1, trace.split(body).size - 1)
    }
}
