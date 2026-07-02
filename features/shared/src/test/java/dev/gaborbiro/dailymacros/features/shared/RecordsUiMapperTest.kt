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
    private val mapper = RecordsUiMapper(NutrientsUiMapper())

    private fun stubTemplate(
        dbId: Long,
        pending: Boolean,
        quickPickOverride: Template.QuickPickOverride?,
    ) = Template(
        dbId = dbId,
        imageFilenames = listOf("a.jpg"),
        isRepresentativeOfMealByImageIndex = emptyList(),
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
        assertEquals(listOf("a.jpg"), ui.imageFilenames)
        assertTrue(ui.showLoadingIndicator)
    }

    @Test
    fun `showOtherLoggedVariantsIcon defaults false`() {
        val ui = mapper.map(record(stubTemplate(1L, false, null)))
        assertFalse(ui.showOtherLoggedVariantsIcon)
    }
}
