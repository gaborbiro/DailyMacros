package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.common.model.TopContributors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class RecordsUiMapperTest {

    private val zone = ZoneId.of("Europe/Paris")
    private val mapper = RecordsUiMapper(TemplateUiMapper())

    private fun stubTemplate(
        dbId: Long,
        pending: Boolean,
        quickPickOverride: Template.QuickPickOverride?,
    ) = Template(
        dbId = dbId,
        imageFilenames = listOf("a.jpg"),
        isRepresentativeOfMealByImageIndex = listOf(null),
        name = "Toast",
        description = "D",
        parentTemplateId = null,
        createdAtEpochMs = 0L,
        updatedAtEpochMs = 0L,
        isPending = pending,
        nutrients = Nutrients(calories = 90),
        notes = "",
        mealComponents = emptyList(),
        topContributors = TopContributors(),
        quickPickOverride = quickPickOverride,
    )

    private fun record(template: Template) = Record(
        recordId = 5L,
        timestamp = ZonedDateTime.of(2024, 3, 15, 14, 7, 0, 0, zone),
        template = template,
    )

    @Test
    fun `map formats full date when not time only`() {
        val ui = mapper.map(record(stubTemplate(1L, false, null)), timeOnly = false)
        assertTrue(ui.timestamp.contains("Mar"))
        assertTrue(ui.timestamp.contains("14:07"))
    }

    @Test
    fun `map formats time only when requested`() {
        val ui = mapper.map(record(stubTemplate(1L, false, null)), timeOnly = true)
        assertEquals("14:07", ui.timestamp)
    }

    @Test
    fun `map passes ids title images and loading flag`() {
        val ui = mapper.map(record(stubTemplate(9L, true, null)))
        assertEquals(5L, ui.recordId)
        assertEquals(9L, ui.templateId)
        assertEquals("Toast", ui.title)
        assertEquals("a.jpg", ui.imageFilename)
        assertTrue(ui.showLoadingIndicator)
    }

    @Test
    fun `showOtherLoggedVariantsIcon defaults false`() {
        val ui = mapper.map(record(stubTemplate(1L, false, null)))
        assertFalse(ui.showOtherLoggedVariantsIcon)
    }

    @Test
    fun `no timezone label when previous record is null`() {
        val ui = mapper.map(record(stubTemplate(1L, false, null)), timeOnly = true)
        assertEquals("14:07", ui.timestamp)
    }

    @Test
    fun `no timezone label when previous record has same zone`() {
        val previous = record(stubTemplate(1L, false, null))
        val ui = mapper.map(record(stubTemplate(1L, false, null)), timeOnly = true, previousRecord = previous)
        assertEquals("14:07", ui.timestamp)
    }

    @Test
    fun `adds positive offset label when zone jumps forward`() {
        val previous = Record(
            recordId = 4L,
            timestamp = ZonedDateTime.of(2024, 3, 15, 10, 0, 0, 0, ZoneId.of("Europe/Lisbon")),
            template = stubTemplate(1L, false, null),
        )
        val current = Record(
            recordId = 5L,
            timestamp = ZonedDateTime.of(2024, 3, 15, 14, 0, 0, 0, ZoneId.of("Asia/Dubai")),
            template = stubTemplate(1L, false, null),
        )
        val ui = mapper.map(current, timeOnly = true, previousRecord = previous)
        assertEquals("14:00 (+4h)", ui.timestamp)
    }

    @Test
    fun `adds negative offset label with hours and minutes when zone jumps backward`() {
        val previous = Record(
            recordId = 4L,
            timestamp = ZonedDateTime.of(2024, 3, 15, 10, 0, 0, 0, ZoneId.of("Asia/Kolkata")),
            template = stubTemplate(1L, false, null),
        )
        val current = Record(
            recordId = 5L,
            timestamp = ZonedDateTime.of(2024, 3, 15, 14, 0, 0, 0, ZoneId.of("Europe/Lisbon")),
            template = stubTemplate(1L, false, null),
        )
        val ui = mapper.map(current, timeOnly = true, previousRecord = previous)
        assertEquals("14:00 (-5h30m)", ui.timestamp)
    }
}
