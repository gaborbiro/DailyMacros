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
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.TimezoneEvent
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
        var timezoneEvents: List<TimezoneEvent> = emptyList()

        override fun getRecentTimezoneEvents(): List<TimezoneEvent> = timezoneEvents

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

    private fun dailySummaryFor(
        records: List<Record>,
        targets: Targets,
        day: java.time.LocalDate,
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
    fun `each leg of a multi-leg trip is scaled on its own marginal shift, not the cumulative shift from departure`() {
        // Home -> a stopover -> final destination, each leg a real (greater than 2hr) shift.
        val home = ZoneOffset.UTC
        val stopover = ZoneOffset.ofHours(3)
        val dest = ZoneOffset.ofHours(9)

        val day1 = stubRecordAt(1L, 300, ZonedDateTime.of(2024, 5, 10, 20, 0, 0, 0, home))
        val day2 = stubRecordAt(2L, 300, ZonedDateTime.of(2024, 5, 11, 9, 0, 0, 0, stopover))
        val day3 = stubRecordAt(3L, 1250, ZonedDateTime.of(2024, 5, 12, 9, 0, 0, 0, dest))

        val day2Summary = dailySummaryFor(listOf(day1, day2, day3), caloriesOnlyTargets, day2.timestamp.toLocalDate())
        val day3Summary = dailySummaryFor(listOf(day1, day2, day3), caloriesOnlyTargets, day3.timestamp.toLocalDate())

        // Day 2: home -> stopover is a 3hr shift.
        assertTrue(day2Summary.infoMessage.orEmpty().contains("3 hrs behind"))
        // Day 3: stopover -> destination is only a 6hr shift (marginal), not the cumulative
        // 9hr from home -- otherwise the stopover's 3hrs would be counted twice.
        assertTrue(day3Summary.infoMessage.orEmpty().contains("6 hrs behind"))
        assertFalse(day3Summary.infoMessage.orEmpty().contains("9 hrs"))
        // scaled to an 18hr day: min=1200, max=1500 -> total(1250) is partway through the range
        assertEquals(0.7917f, day3Summary.entries.single().progress0to1, 0.001f)
    }

    @Test
    fun `a zone blip of 2hrs or less is ignored and does not become the new anchor`() {
        val home = ZoneOffset.UTC
        val blip = ZoneOffset.ofHours(2)
        val farther = ZoneOffset.ofHours(5)

        val day1 = stubRecordAt(1L, 300, ZonedDateTime.of(2024, 5, 10, 20, 0, 0, 0, home))
        // A 2hr blip -- at the threshold, not past it, so it should be silently ignored.
        val day2 = stubRecordAt(2L, 300, ZonedDateTime.of(2024, 5, 11, 9, 0, 0, 0, blip))
        val day3 = stubRecordAt(3L, 300, ZonedDateTime.of(2024, 5, 12, 9, 0, 0, 0, farther))

        val day2Summary = dailySummaryFor(listOf(day1, day2, day3), caloriesOnlyTargets, day2.timestamp.toLocalDate())
        val day3Summary = dailySummaryFor(listOf(day1, day2, day3), caloriesOnlyTargets, day3.timestamp.toLocalDate())

        assertNull(day2Summary.infoMessage)
        // If the 2hr blip had wrongly become the new anchor, day 3 would read as a 3hr shift
        // (blip -> farther) instead of the correct 5hr shift measured from home.
        assertTrue(day3Summary.infoMessage.orEmpty().contains("5 hrs behind"))
    }

    @Test
    fun `a plain day fully in the new zone does not repeat the previous day's shift`() {
        val home = ZoneOffset.UTC
        val dest = ZoneOffset.ofHours(9)

        val day1 = stubRecordAt(1L, 300, ZonedDateTime.of(2024, 5, 10, 20, 0, 0, 0, home))
        val day2 = stubRecordAt(2L, 300, ZonedDateTime.of(2024, 5, 11, 9, 0, 0, 0, dest))
        // A plain day after arrival, no shift of its own -- just continuing the same zone.
        val day3 = stubRecordAt(3L, 300, ZonedDateTime.of(2024, 5, 12, 17, 0, 0, 0, dest))

        val day2Summary = dailySummaryFor(listOf(day1, day2, day3), caloriesOnlyTargets, day2.timestamp.toLocalDate())
        val day3Summary = dailySummaryFor(listOf(day1, day2, day3), caloriesOnlyTargets, day3.timestamp.toLocalDate())

        assertTrue(day2Summary.infoMessage.orEmpty().contains("9 hrs behind"))
        assertNull(day3Summary.infoMessage)
    }

    @Test
    fun `a detected timezone event surfaces the advisory on a day with no logged meals`() {
        val home = ZoneOffset.UTC
        val dest = ZoneOffset.ofHours(9)

        val day1 = stubRecordAt(1L, 300, ZonedDateTime.of(2024, 5, 10, 20, 0, 0, 0, home))
        // Nothing logged on day 2 (the actual travel day) -- only a detected OS timezone event.
        testSettingsRepository.timezoneEvents = listOf(
            TimezoneEvent(
                epochMs = ZonedDateTime.of(2024, 5, 11, 9, 0, 0, 0, dest).toInstant().toEpochMilli(),
                zoneId = dest.id,
            )
        )
        val day3 = stubRecordAt(3L, 300, ZonedDateTime.of(2024, 5, 12, 17, 0, 0, 0, dest))

        val phantomDaySummary = dailySummaryFor(
            listOf(day1, day3),
            caloriesOnlyTargets,
            java.time.LocalDate.of(2024, 5, 11),
        )
        val day3Summary = dailySummaryFor(listOf(day1, day3), caloriesOnlyTargets, day3.timestamp.toLocalDate())

        assertTrue(phantomDaySummary.infoMessage.orEmpty().contains("9 hrs behind"))
        // Day 3's shift is now measured from the phantom day's zone, not re-triggering the
        // already-anchored 9hr jump.
        assertNull(day3Summary.infoMessage)
    }

    @Test
    fun `a timezone event landing on an already-logged day is ignored`() {
        val home = ZoneOffset.UTC
        val dest = ZoneOffset.ofHours(9)

        val day1 = stubRecordAt(1L, 300, ZonedDateTime.of(2024, 5, 10, 20, 0, 0, 0, home))
        val day2 = stubRecordAt(2L, 300, ZonedDateTime.of(2024, 5, 11, 9, 0, 0, 0, dest))
        // A redundant event landing on a day that already has a real record.
        testSettingsRepository.timezoneEvents = listOf(
            TimezoneEvent(
                epochMs = ZonedDateTime.of(2024, 5, 11, 12, 0, 0, 0, dest).toInstant().toEpochMilli(),
                zoneId = dest.id,
            )
        )

        val out = mapper.map(listOf(day1, day2), caloriesOnlyTargets)
            .filterIsInstance<ListUiModelDailySummary>()

        // Exactly one summary card per day -- the event didn't spawn a duplicate phantom day.
        assertEquals(2, out.size)
    }
}
