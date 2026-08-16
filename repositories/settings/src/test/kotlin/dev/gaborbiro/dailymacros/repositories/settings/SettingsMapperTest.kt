package dev.gaborbiro.dailymacros.repositories.settings

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SettingsMapperTest {

    private val mapper = SettingsMapper(Gson())

    @Test
    fun `field missing from persisted json falls back to default target instead of null`() {
        val json = """
            {
                "protein": {"enabled": true, "min": 50, "max": 150},
                "salt": {"enabled": false}
            }
        """.trimIndent()

        val targets = mapper.map(json)

        assertNotNull(targets.calories)
        assertEquals(false, targets.calories.enabled)
        assertEquals(true, targets.protein.enabled)
        assertEquals(50, targets.protein.min)
        assertEquals(150, targets.protein.max)
    }

    @Test
    fun `malformed json falls back to defaults for every target`() {
        val targets = mapper.map("not valid json")

        assertEquals(false, targets.calories.enabled)
        assertEquals(false, targets.protein.enabled)
        assertEquals(false, targets.salt.enabled)
        assertEquals(false, targets.fat.enabled)
        assertEquals(false, targets.carbs.enabled)
        assertEquals(false, targets.fibre.enabled)
        assertEquals(false, targets.ofWhichSaturated.enabled)
        assertEquals(false, targets.ofWhichSugar.enabled)
    }

    @Test
    fun `round trip through toJson preserves values`() {
        val json = mapper.map(
            dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets(
                calories = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target(enabled = true, min = 1500, max = 2500),
                protein = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target(enabled = false),
                salt = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target(enabled = false),
                fat = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target(enabled = false),
                carbs = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target(enabled = false),
                fibre = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target(enabled = false),
                ofWhichSaturated = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target(enabled = false),
                ofWhichSugar = dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target(enabled = false),
            )
        )

        val targets = mapper.map(json)

        assertEquals(true, targets.calories.enabled)
        assertEquals(1500, targets.calories.min)
        assertEquals(2500, targets.calories.max)
    }
}
