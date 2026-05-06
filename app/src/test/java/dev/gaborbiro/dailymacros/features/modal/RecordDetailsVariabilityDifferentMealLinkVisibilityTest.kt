package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityPreviewContent
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

    private fun previewWithSlots(label: String = "Toast breakfast") = TemplateVariabilityPreviewContent(
        bannerText = "",
        archetypePickerLabel = label,
        slots = listOf(
            TemplateVariabilitySlotPreview(
                archetypeKey = "a",
                archetypeDisplayName = "A",
                slotKey = "s",
                roleDisplayName = "S",
                variants = listOf(
                    TemplateVariabilityVariantPreview("v", "V"),
                ),
            ),
        ),
    )

    @Test
    fun `false when not editable`() {
        assertFalse(
            computeShowVariabilityDifferentMealLink(
                allowEdit = false,
                templateVariabilityPreview = previewWithSlots(),
                variabilityArchetypes = stubArchetypes(),
            ),
        )
    }

    @Test
    fun `false when preview null`() {
        assertFalse(
            computeShowVariabilityDifferentMealLink(
                allowEdit = true,
                templateVariabilityPreview = null,
                variabilityArchetypes = stubArchetypes(),
            ),
        )
    }

    @Test
    fun `false when slots empty`() {
        assertFalse(
            computeShowVariabilityDifferentMealLink(
                allowEdit = true,
                templateVariabilityPreview = TemplateVariabilityPreviewContent(
                    bannerText = "",
                    archetypePickerLabel = "X",
                    slots = emptyList(),
                ),
                variabilityArchetypes = stubArchetypes(),
            ),
        )
    }

    @Test
    fun `false when archetype label blank`() {
        assertFalse(
            computeShowVariabilityDifferentMealLink(
                allowEdit = true,
                templateVariabilityPreview = previewWithSlots(label = "   "),
                variabilityArchetypes = stubArchetypes(),
            ),
        )
    }

    @Test
    fun `false when variability archetypes empty`() {
        assertFalse(
            computeShowVariabilityDifferentMealLink(
                allowEdit = true,
                templateVariabilityPreview = previewWithSlots(),
                variabilityArchetypes = emptyList(),
            ),
        )
    }

    @Test
    fun `true when editable preview and archetypes ok`() {
        assertTrue(
            computeShowVariabilityDifferentMealLink(
                allowEdit = true,
                templateVariabilityPreview = previewWithSlots(),
                variabilityArchetypes = stubArchetypes(),
            ),
        )
    }
}
