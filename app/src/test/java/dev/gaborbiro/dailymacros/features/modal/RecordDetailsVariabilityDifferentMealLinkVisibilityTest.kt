package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.features.modal.model.VariabilityArchetypePickerEntry
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilitySlotPreview
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityVariantPreview
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityArchetype
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordDetailsVariabilityDifferentMealLinkVisibilityTest {

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
    fun `false when not editable`() {
        assertFalse(
            computeShowVariabilityDifferentMealLink(
                allowEdit = false,
                variabilityArchetypePickerEntries = stubEntries(),
                variabilityArchetypes = stubArchetypes(),
            ),
        )
    }

    @Test
    fun `false when entries empty`() {
        assertFalse(
            computeShowVariabilityDifferentMealLink(
                allowEdit = true,
                variabilityArchetypePickerEntries = emptyList(),
                variabilityArchetypes = stubArchetypes(),
            ),
        )
    }

    @Test
    fun `false when archetypes empty`() {
        assertFalse(
            computeShowVariabilityDifferentMealLink(
                allowEdit = true,
                variabilityArchetypePickerEntries = stubEntries(),
                variabilityArchetypes = emptyList(),
            ),
        )
    }

    @Test
    fun `true when editable entries and archetypes ok`() {
        assertTrue(
            computeShowVariabilityDifferentMealLink(
                allowEdit = true,
                variabilityArchetypePickerEntries = stubEntries(),
                variabilityArchetypes = stubArchetypes(),
            ),
        )
    }
}
