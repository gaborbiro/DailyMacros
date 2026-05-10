package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.shared.NutrientsUiMapper
import dev.gaborbiro.dailymacros.features.shared.model.NutrientBreakdown
import dev.gaborbiro.dailymacros.features.modal.model.VariabilityArchetypePickerEntry
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Template
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateNutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilitySlotPreview
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityVariantPreview
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TopContributors
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.DomainError
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityArchetype
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class ModalUiMapperTest {

    private val zone = ZoneId.of("UTC")
    private val mapper = ModalUiMapper(NutrientsUiMapper())

    private fun stubArchetypes() = listOf(
        VariabilityArchetype(
            archetypeKey = "a",
            displayName = "A",
            titleAliasesJson = "[]",
            evidenceCount = 0,
            lastSeenTimestamp = null,
            archetypeNotes = null,
            deprecated = false,
            deprecatedReason = null,
            slots = emptyList(),
            sortOrder = 0,
        ),
    )

    private fun stubEntries() = listOf(
        VariabilityArchetypePickerEntry(
            archetypeKey = "a",
            linkTitle = "A",
            slots = listOf(
                TemplateVariabilitySlotPreview(
                    archetypeKey = "a",
                    archetypeDisplayName = "A",
                    slotKey = "s",
                    roleDisplayName = "S",
                    variants = listOf(TemplateVariabilityVariantPreview("v", "V")),
                ),
            ),
        ),
    )

    @Test
    fun `mapShowVariabilityDifferentMealLink false when not editable`() {
        assertFalse(
            mapper.mapShowVariabilityDifferentMealLink(
                allowEdit = false,
                variabilityArchetypePickerEntries = stubEntries(),
                variabilityArchetypes = stubArchetypes(),
            ),
        )
    }

    @Test
    fun `mapShowVariabilityDifferentMealLink false when entries empty`() {
        assertFalse(
            mapper.mapShowVariabilityDifferentMealLink(
                allowEdit = true,
                variabilityArchetypePickerEntries = emptyList(),
                variabilityArchetypes = stubArchetypes(),
            ),
        )
    }

    @Test
    fun `mapShowVariabilityDifferentMealLink false when archetypes empty`() {
        assertFalse(
            mapper.mapShowVariabilityDifferentMealLink(
                allowEdit = true,
                variabilityArchetypePickerEntries = stubEntries(),
                variabilityArchetypes = emptyList(),
            ),
        )
    }

    @Test
    fun `mapShowVariabilityDifferentMealLink true when editable entries and archetypes ok`() {
        assertTrue(
            mapper.mapShowVariabilityDifferentMealLink(
                allowEdit = true,
                variabilityArchetypePickerEntries = stubEntries(),
                variabilityArchetypes = stubArchetypes(),
            ),
        )
    }

    @Test
    fun `mapNutrientBreakdowns formats calories and notes`() {
        val record = Record(
            recordId = 1L,
            timestamp = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, zone),
            template = Template(
                dbId = 1L,
                images = emptyList(),
                isRepresentativeOfMealByImageIndex = emptyList(),
                name = "Meal",
                description = "D",
                parentTemplateId = null,
                createdAtEpochMs = 0L,
                updatedAtEpochMs = 0L,
                isPending = false,
                nutrients = TemplateNutrientBreakdown(calories = 300, protein = 20f),
                notes = "Leftovers",
                mealComponents = emptyList(),
                topContributors = TopContributors(),
                quickPickOverride = null,
            ),
        )
        val ui = mapper.mapNutrientBreakdowns(record)
        assertNotNull(ui.calories)
        assertTrue(ui.calories!!.contains("300"))
        assertNotNull(ui.protein)
        assertEquals("Leftovers", ui.notes)
    }

    @Test
    fun `mapMacrosPrintout null when breakdown empty`() {
        assertNull(mapper.mapMacrosPrintout(NutrientBreakdown()))
    }

    @Test
    fun `mapMacrosPrintout joins non blank lines`() {
        val s = mapper.mapMacrosPrintout(
            NutrientBreakdown(calories = 100, protein = 5f, salt = 0.5f),
        )
        assertNotNull(s)
        assertTrue(s!!.contains("100"))
        assertTrue(s.contains("5"))
    }

    @Test
    fun `mapDomainErrorToUserMessage check internet`() {
        assertEquals(
            "Internet connectivity error",
            mapper.mapDomainErrorToUserMessage(DomainError.DisplayMessageToUser.CheckInternetConnection()),
        )
    }

    @Test
    fun `mapDomainErrorToUserMessage contact support`() {
        assertTrue(
            mapper.mapDomainErrorToUserMessage(DomainError.DisplayMessageToUser.ContactSupport())
                .contains("engineers"),
        )
    }

    @Test
    fun `mapDomainErrorToUserMessage custom message`() {
        assertEquals(
            "Hello",
            mapper.mapDomainErrorToUserMessage(DomainError.DisplayMessageToUser.Message("Hello")),
        )
    }

    @Test
    fun `mapDomainErrorToUserMessage try again`() {
        assertTrue(
            mapper.mapDomainErrorToUserMessage(DomainError.DisplayMessageToUser.TryAgain())
                .contains("try again"),
        )
    }
}
