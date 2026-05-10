package dev.gaborbiro.dailymacros.features.shared

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class ChatGptOkHttpTimeoutsTest {

    @Test
    fun `applyImageUploadTimeouts sets expected timeouts`() {
        val client = OkHttpClient.Builder()
            .apply(ChatGptOkHttpTimeouts::applyImageUploadTimeouts)
            .build()
        assertEquals(TimeUnit.SECONDS.toMillis(ChatGptOkHttpTimeouts.CONNECT_SECONDS).toInt(), client.connectTimeoutMillis)
        assertEquals(TimeUnit.SECONDS.toMillis(ChatGptOkHttpTimeouts.WRITE_SECONDS_WITH_IMAGES).toInt(), client.writeTimeoutMillis)
        assertEquals(TimeUnit.SECONDS.toMillis(ChatGptOkHttpTimeouts.READ_SECONDS).toInt(), client.readTimeoutMillis)
        assertEquals(TimeUnit.SECONDS.toMillis(ChatGptOkHttpTimeouts.CALL_SECONDS).toInt(), client.callTimeoutMillis)
    }

    @Test
    fun `applyJsonBodyTimeouts sets shorter write timeout than image uploads`() {
        val client = OkHttpClient.Builder()
            .apply(ChatGptOkHttpTimeouts::applyJsonBodyTimeouts)
            .build()
        assertEquals(TimeUnit.SECONDS.toMillis(ChatGptOkHttpTimeouts.WRITE_SECONDS_JSON_BODY).toInt(), client.writeTimeoutMillis)
        assertEquals(TimeUnit.SECONDS.toMillis(ChatGptOkHttpTimeouts.READ_SECONDS).toInt(), client.readTimeoutMillis)
        assertEquals(TimeUnit.SECONDS.toMillis(ChatGptOkHttpTimeouts.CALL_SECONDS).toInt(), client.callTimeoutMillis)
    }
}
