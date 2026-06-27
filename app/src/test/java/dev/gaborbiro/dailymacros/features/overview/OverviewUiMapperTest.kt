package dev.gaborbiro.dailymacros.features.overview

import dev.gaborbiro.dailymacros.features.shared.NutrientsUiMapper
import dev.gaborbiro.dailymacros.features.shared.RecordsMapper
import dev.gaborbiro.dailymacros.features.shared.SharedRecordsUiMapper
import dev.gaborbiro.dailymacros.features.overview.model.ChangeDirection
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class OverviewUiMapperTest {

    private val zone = ZoneId.of("UTC")

    private val disabledTarget = Target(enabled = false)

    private val testSettingsRepository = object : SettingsRepository {
        override fun getTargets(): Targets = Targets(
            calories = disabledTarget,
            protein = disabledTarget,
            salt = disabledTarget,
            fat = disabledTarget,
            carbs = disabledTarget,
            fibre = disabledTarget,
            ofWhichSaturated = disabledTarget,
            ofWhichSugar = disabledTarget,
        )

        override fun setTargets(targets: Targets) = Unit

        override fun getDiaryDayStartHour(): Int = 0

        override fun setDiaryDayStartHour(hourOfDay: Int) = Unit

        override fun getPromptCustomizations(): Map<String, String> = emptyMap()
        override fun setPromptCustomizations(values: Map<String, String>) = Unit
        override fun clearPromptCustomizations() = Unit
        override fun getPromptVersions(type: String) = emptyList<dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion>()
        override fun savePromptVersion(type: String, customizations: Map<String, String>) = dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion(1, 0L, emptyMap())
        override fun deletePromptVersion(version: Int) = Unit
        override fun getApiKeyOverride(): String? = null
        override fun setApiKeyOverride(key: String) = Unit
        override fun clearApiKeyOverride() = Unit
    }

    private val mapper = OverviewUiMapper(
        recordsUiMapper = SharedRecordsUiMapper(NutrientsUiMapper()),
        nutrientsUiMapper = NutrientsUiMapper(),
        recordsMapper = RecordsMapper(),
        settingsRepository = testSettingsRepository,
    )

    private fun stubTemplate(dbId: Long, name: String) = Template(
        dbId = dbId,
        images = emptyList(),
        isRepresentativeOfMealByImageIndex = emptyList(),
        name = name,
        description = "d",
        parentTemplateId = null,
        createdAtEpochMs = 0L,
        updatedAtEpochMs = 0L,
        isPending = false,
        nutrients = TemplateNutrientBreakdown(calories = 100),
        notes = "",
        mealComponents = emptyList(),
        topContributors = TopContributors(),
        quickPickOverride = null,
    )

    private fun stubRecord(id: Long, template: Template, hour: Int) = Record(
        recordId = id,
        timestamp = ZonedDateTime.of(2024, 5, 10, hour, 0, 0, 0, zone),
        template = template,
    )

    @Test
    fun `mapSearchResults reverses mapped records`() {
        val r1 = stubRecord(1L, stubTemplate(10L, "First"), 8)
        val r2 = stubRecord(2L, stubTemplate(20L, "Second"), 9)
        val out = mapper.mapSearchResults(listOf(r1, r2))
        assertEquals(2, out.size)
        assertEquals(2L, out[0].listItemId)
        assertEquals(1L, out[1].listItemId)
    }

    @Test
    fun `calculateChangeIndicator neutral when current or previous missing or zero`() {
        assertEquals(ChangeDirection.NEUTRAL, mapper.calculateChangeIndicator(null, 10f).direction)
        assertEquals(ChangeDirection.NEUTRAL, mapper.calculateChangeIndicator(0f, 10f).direction)
        assertEquals(ChangeDirection.NEUTRAL, mapper.calculateChangeIndicator(10f, null).direction)
        assertEquals(ChangeDirection.NEUTRAL, mapper.calculateChangeIndicator(10f, 0f).direction)
    }

    @Test
    fun `calculateChangeIndicator up when more than two percent increase`() {
        val c = mapper.calculateChangeIndicator(105f, 100f)
        assertEquals(ChangeDirection.UP, c.direction)
        assertTrue(c.value.startsWith("+"))
    }

    @Test
    fun `calculateChangeIndicator down when more than two percent decrease`() {
        val c = mapper.calculateChangeIndicator(90f, 100f)
        assertEquals(ChangeDirection.DOWN, c.direction)
        assertTrue(c.value.startsWith("-"))
    }

    @Test
    fun `calculateChangeIndicator neutral within two percent band`() {
        assertEquals(ChangeDirection.NEUTRAL, mapper.calculateChangeIndicator(102f, 100f).direction)
        assertEquals(ChangeDirection.NEUTRAL, mapper.calculateChangeIndicator(98f, 100f).direction)
    }
}
