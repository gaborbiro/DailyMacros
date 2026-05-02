package dev.gaborbiro.dailymacros.features.common

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * OkHttp timeouts for OpenAI HTTP calls. These are intentionally asymmetric:
 * - **Connect** only covers TCP/TLS setup (short).
 * - **Write** covers uploading the request (JSON vs large base64 image payloads).
 * - **Read** is the long pole while the model generates before bytes arrive on the socket.
 * - **Call** is a hard wall-clock cap for the entire exchange (must fit connect + write + read).
 */
internal object ChatGptOkHttpTimeouts {
    const val CONNECT_SECONDS = 30L
    const val READ_SECONDS = 900L
    const val CALL_SECONDS = 1500L

    /** Nutrient / food flows that upload one or more base64 images. */
    const val WRITE_SECONDS_WITH_IMAGES = 240L

    /** JSON-only requests (e.g. meal variability mining). */
    const val WRITE_SECONDS_JSON_BODY = 120L

    fun applyImageUploadTimeouts(builder: OkHttpClient.Builder) {
        builder
            .connectTimeout(CONNECT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_SECONDS_WITH_IMAGES, TimeUnit.SECONDS)
            .readTimeout(READ_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_SECONDS, TimeUnit.SECONDS)
    }

    fun applyJsonBodyTimeouts(builder: OkHttpClient.Builder) {
        builder
            .connectTimeout(CONNECT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_SECONDS_JSON_BODY, TimeUnit.SECONDS)
            .readTimeout(READ_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_SECONDS, TimeUnit.SECONDS)
    }
}
