package dev.gaborbiro.dailymacros.repositories.records

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class VariabilityProfileMapperParseTest {

    private val mapper = VariabilityProfileMapper(Gson())

    @Test
    fun `parseProfileJson preserves templates ingest watermark`() {
        val profile = mapper.parseProfileJson(
            profileJson = """{"archetypes":[]}""",
            minedAtEpochMs = 100L,
            templatesIngestWatermarkEpochMs = 777L,
        )
        assertEquals(777L, profile.templatesIngestWatermarkEpochMs)
        assertEquals(100L, profile.minedAtEpochMs)
    }
}
