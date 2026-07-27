package dev.gaborbiro.dailymacros.features.overview

import dev.gaborbiro.dailymacros.features.shared.TemplateUiMapper
import dev.gaborbiro.dailymacros.features.shared.RecordsUiMapper
import dev.gaborbiro.dailymacros.features.overview.model.ChangeDirection
import dev.gaborbiro.dailymacros.features.overview.model.ListUiModelDailySummary
import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelRecord
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.common.model.TopContributors
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Target
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import android.content.Context
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.ZoneOffset
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

        override fun getPromptCustomisations(): Map<String, String> = emptyMap()
        override fun setPromptCustomisations(values: Map<String, String>) = Unit
        override fun clearPromptCustomisations() = Unit
        override fun getPromptVersions(type: String) = emptyList<dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion>()
        override fun savePromptVersion(type: String, customisations: Map<String, String>) = dev.gaborbiro.dailymacros.repositories.settings.domain.model.PromptVersion(1, 0L, emptyMap())
        override fun deletePromptVersion(version: Int) = Unit
        override fun getApiKeyOverride(): String? = null
        override fun setApiKeyOverride(key: String) = Unit
        override fun clearApiKeyOverride() = Unit
    }

    private val context: Context get() = RuntimeEnvironment.getApplication()

    private val mapper get() = OverviewUiMapper(
        context = context,
        recordsUiMapper = RecordsUiMapper(TemplateUiMapper()),
        templateUiMapper = TemplateUiMapper(),
        settingsRepository = testSettingsRepository,
    )

    private fun stubTemplate(dbId: Long, name: String) = Template(
        dbId = dbId,
        imageFilenames = emptyList(),
        isRepresentativeOfMealByImageIndex = emptyList(),
        name = name,
        description = "d",
        parentTemplateId = null,
        createdAtEpochMs = 0L,
        updatedAtEpochMs = 0L,
        isPending = false,
        nutrients = Nutrients(calories = 100),
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

    private fun stubRecordAt(id: Long, calories: Int, timestamp: ZonedDateTime) = Record(
        recordId = id,
        timestamp = timestamp,
        template = stubTemplate(id, "R$id").copy(nutrients = Nutrients(calories = calories)),
    )

    private val caloriesOnlyTargets = Targets(
        calories = Target(enabled = true, min = 1600, max = 2000),
        protein = disabledTarget,
        salt = disabledTarget,
        fat = disabledTarget,
        carbs = disabledTarget,
        fibre = disabledTarget,
        ofWhichSaturated = disabledTarget,
        ofWhichSugar = disabledTarget,
    )

    private fun mapperWithAdaptationHours(hours: Int) = OverviewUiMapper(
        context = context,
        recordsUiMapper = RecordsUiMapper(TemplateUiMapper()),
        templateUiMapper = TemplateUiMapper(),
        settingsRepository = object : SettingsRepository by testSettingsRepository {
            override fun getTimezoneAdaptationHours(): Int = hours
        },
    )

    private fun dailySummaryFor(
        records: List<Record>,
        targets: Targets,
        day: java.time.LocalDate,
        mapper: OverviewUiMapper = this.mapper,
    ): ListUiModelDailySummary =
        mapper.map(records, targets)
            .filterIsInstance<ListUiModelDailySummary>()
            .first { it.dayTitle.endsWith(day.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM"))) }

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
    fun `mapSearchResults adds timezone shift label when zone changes between records`() {
        val r1 = Record(
            recordId = 1L,
            timestamp = ZonedDateTime.of(2024, 5, 10, 8, 0, 0, 0, ZoneId.of("Europe/Lisbon")),
            template = stubTemplate(10L, "First"),
        )
        val r2 = Record(
            recordId = 2L,
            timestamp = ZonedDateTime.of(2024, 5, 10, 14, 0, 0, 0, ZoneId.of("Asia/Dubai")),
            template = stubTemplate(20L, "Second"),
        )
        val out = mapper.mapSearchResults(listOf(r1, r2)).filterIsInstance<ListUiModelRecord>()
        val second = out.first { it.listItemId == 2L }
        assertTrue(second.timestamp.contains("(+3h)"))
    }

    @Test
    fun `map adds timezone shift label when zone changes between records`() {
        val r1 = Record(
            recordId = 1L,
            timestamp = ZonedDateTime.of(2024, 5, 10, 8, 0, 0, 0, ZoneId.of("Europe/Lisbon")),
            template = stubTemplate(10L, "First"),
        )
        val r2 = Record(
            recordId = 2L,
            timestamp = ZonedDateTime.of(2024, 5, 10, 14, 0, 0, 0, ZoneId.of("Asia/Dubai")),
            template = stubTemplate(20L, "Second"),
        )
        val out = mapper.map(listOf(r1, r2), testSettingsRepository.getTargets())
            .filterIsInstance<ListUiModelRecord>()
        val first = out.first { it.listItemId == 1L }
        val second = out.first { it.listItemId == 2L }
        assertFalse(first.timestamp.contains("("))
        assertTrue(second.timestamp.contains("(+3h)"))
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

    @Test
    fun `day summary is not scaled when there is no timezone shift`() {
        val day1 = stubRecordAt(1L, 300, ZonedDateTime.of(2024, 5, 10, 20, 0, 0, 0, ZoneOffset.UTC))
        val day2 = stubRecordAt(2L, 1500, ZonedDateTime.of(2024, 5, 11, 9, 0, 0, 0, ZoneOffset.UTC))

        val summary = dailySummaryFor(listOf(day1, day2), caloriesOnlyTargets, day2.timestamp.toLocalDate())

        assertNull(summary.infoMessage)
        // unscaled: total(1500) <= min(1600) -> (1500/1600) * 0.75
        assertEquals(0.703125f, summary.entries.single().progress0to1, 0.001f)
    }

    @Test
    fun `day summary is scaled down for a timezone shift that happens entirely overnight`() {
        // Last log of day 1 is still in the old zone; day 2's only log is already in the new
        // (arrival) zone -- the shift happened in the gap between the two, not during either day.
        val day1 = stubRecordAt(1L, 300, ZonedDateTime.of(2024, 5, 10, 20, 0, 0, 0, ZoneOffset.UTC))
        val day2 = stubRecordAt(2L, 1500, ZonedDateTime.of(2024, 5, 11, 9, 0, 0, 0, ZoneOffset.ofHours(6)))

        val summary = dailySummaryFor(listOf(day1, day2), caloriesOnlyTargets, day2.timestamp.toLocalDate())

        assertTrue(summary.infoMessage.orEmpty().contains("6 hrs behind"))
        assertTrue(summary.infoMessage.orEmpty().contains("shorter day"))
        // scaled to an 18hr day: min=1200, max=1500 -> total(1500) is at the top of the scaled range
        assertEquals(1.0f, summary.entries.single().progress0to1, 0.001f)
    }

    @Test
    fun `a short layover keeps tracking cumulative shift at the default adaptation threshold`() {
        // Home -> a brief stopover (a single log, no real dwell time) -> final destination.
        val home = ZoneOffset.UTC
        val stopover = ZoneOffset.ofHours(3)
        val dest = ZoneOffset.ofHours(9)

        val day1 = stubRecordAt(1L, 300, ZonedDateTime.of(2024, 5, 10, 20, 0, 0, 0, home))
        val day2 = stubRecordAt(2L, 300, ZonedDateTime.of(2024, 5, 11, 9, 0, 0, 0, stopover))
        val day3 = stubRecordAt(3L, 1250, ZonedDateTime.of(2024, 5, 12, 9, 0, 0, 0, dest))

        val summary = dailySummaryFor(listOf(day1, day2, day3), caloriesOnlyTargets, day3.timestamp.toLocalDate())

        // The stopover log has ~0hrs of measured dwell, well under the default 20hr threshold,
        // so day 3 is still compared against home (the last genuinely-settled zone) -- the full
        // 9hr difference, not just the 6hr of the final leg.
        assertTrue(summary.infoMessage.orEmpty().contains("9 hrs behind"))
        // scaled to a 15hr day: min=1000, max=1250 -> total(1250) is at the top of the range
        assertEquals(1.0f, summary.entries.single().progress0to1, 0.001f)
    }

    @Test
    fun `lowering the adaptation threshold lets a short layover reset the baseline`() {
        val home = ZoneOffset.UTC
        val stopover = ZoneOffset.ofHours(3)
        val dest = ZoneOffset.ofHours(9)

        val day1 = stubRecordAt(1L, 300, ZonedDateTime.of(2024, 5, 10, 20, 0, 0, 0, home))
        val day2 = stubRecordAt(2L, 300, ZonedDateTime.of(2024, 5, 11, 9, 0, 0, 0, stopover))
        val day3 = stubRecordAt(3L, 1250, ZonedDateTime.of(2024, 5, 12, 9, 0, 0, 0, dest))

        val summary = dailySummaryFor(
            listOf(day1, day2, day3),
            caloriesOnlyTargets,
            day3.timestamp.toLocalDate(),
            mapper = mapperWithAdaptationHours(0),
        )

        // With the threshold set to 0, any logged presence in the stopover counts as settled,
        // so day 3 only reflects the final 6hr leg (stopover -> destination).
        assertTrue(summary.infoMessage.orEmpty().contains("6 hrs behind"))
        // scaled to an 18hr day: min=1200, max=1500 -> total(1250) is partway through the range
        assertEquals(0.7917f, summary.entries.single().progress0to1, 0.001f)
    }

    @Test
    fun `a stopover long enough to clear the default threshold resets the baseline on its own`() {
        val home = ZoneOffset.UTC
        val stopover = ZoneOffset.ofHours(3)
        val dest = ZoneOffset.ofHours(9)

        val day1 = stubRecordAt(1L, 300, ZonedDateTime.of(2024, 5, 10, 20, 0, 0, 0, home))
        // Two logs 22hrs apart in the stopover zone -- a genuine overnight stay, clearing the
        // default 20hr threshold before the final leg begins.
        val day2a = stubRecordAt(2L, 300, ZonedDateTime.of(2024, 5, 11, 9, 0, 0, 0, stopover))
        val day2b = stubRecordAt(3L, 300, ZonedDateTime.of(2024, 5, 12, 7, 0, 0, 0, stopover))
        val day3 = stubRecordAt(4L, 1250, ZonedDateTime.of(2024, 5, 13, 9, 0, 0, 0, dest))

        val summary = dailySummaryFor(
            listOf(day1, day2a, day2b, day3),
            caloriesOnlyTargets,
            day3.timestamp.toLocalDate(),
        )

        assertTrue(summary.infoMessage.orEmpty().contains("6 hrs behind"))
        assertEquals(0.7917f, summary.entries.single().progress0to1, 0.001f)
    }
}
