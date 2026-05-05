package dev.gaborbiro.dailymacros.features.modal

import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityPreviewContent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilitySlotPreview
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityVariantPreview
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordDetailsVariabilityDifferentMealLinkVisibilityTest {

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
                variabilityProfileJson = "{}",
            ),
        )
    }

    @Test
    fun `false when preview null`() {
        assertFalse(
            computeShowVariabilityDifferentMealLink(
                allowEdit = true,
                templateVariabilityPreview = null,
                variabilityProfileJson = "{}",
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
                variabilityProfileJson = "{}",
            ),
        )
    }

    @Test
    fun `false when archetype label blank`() {
        assertFalse(
            computeShowVariabilityDifferentMealLink(
                allowEdit = true,
                templateVariabilityPreview = previewWithSlots(label = "   "),
                variabilityProfileJson = "{}",
            ),
        )
    }

    @Test
    fun `false when profile json null or blank`() {
        assertFalse(
            computeShowVariabilityDifferentMealLink(
                allowEdit = true,
                templateVariabilityPreview = previewWithSlots(),
                variabilityProfileJson = null,
            ),
        )
        assertFalse(
            computeShowVariabilityDifferentMealLink(
                allowEdit = true,
                templateVariabilityPreview = previewWithSlots(),
                variabilityProfileJson = "",
            ),
        )
        assertFalse(
            computeShowVariabilityDifferentMealLink(
                allowEdit = true,
                templateVariabilityPreview = previewWithSlots(),
                variabilityProfileJson = "   ",
            ),
        )
    }

    @Test
    fun `true when editable and preview and json ok`() {
        assertTrue(
            computeShowVariabilityDifferentMealLink(
                allowEdit = true,
                templateVariabilityPreview = previewWithSlots(),
                variabilityProfileJson = "{}",
            ),
        )
    }
}
