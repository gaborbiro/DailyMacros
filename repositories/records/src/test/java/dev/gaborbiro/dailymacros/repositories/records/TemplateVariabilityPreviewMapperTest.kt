package dev.gaborbiro.dailymacros.repositories.records

import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityArchetype
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityEvidence
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilitySlot
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateVariabilityPreviewMapperTest {

    private val mapper = TemplateVariabilityPreviewMapper()

    @Test
    fun `includes archetype slot and variant when template matches evidence`() {
        val archetypes = listOf(
            VariabilityArchetype(
                archetypeKey = "toast_breakfast",
                displayName = "Toast breakfast",
                titleAliasesJson = "[]",
                evidenceCount = 2,
                lastSeenTimestamp = null,
                archetypeNotes = null,
                deprecated = false,
                deprecatedReason = null,
                slots = listOf(
                    VariabilitySlot(
                        slotKey = "spread",
                        roleDisplayName = "Spread",
                        nutritionalLeversJson = "[]",
                        isHighVariability = true,
                        confidence = 0.9,
                        rationale = "",
                        variants = listOf(
                            VariabilityVariant(
                                variantKey = "butter",
                                variantLabel = "Butter",
                                notesExcerpt = "",
                                evidence = listOf(VariabilityEvidence(loggedAt = "x", templateId = 42L)),
                                sortOrder = 0,
                            ),
                            VariabilityVariant(
                                variantKey = "jam",
                                variantLabel = "Jam",
                                notesExcerpt = "",
                                evidence = listOf(VariabilityEvidence(loggedAt = "y", templateId = 99L)),
                                sortOrder = 1,
                            ),
                        ),
                        sortOrder = 0,
                    ),
                ),
                sortOrder = 0,
            ),
        )
        val lines = mapper.toPreviewLines(archetypes, templateId = 42L)
        assertTrue(lines.any { it.contains("Toast breakfast") && it.contains("toast_breakfast") })
        assertTrue(lines.any { it.contains("Spread") && it.contains("spread") })
        assertTrue(lines.any { it.contains("Butter") && it.contains("butter") })

        val slots = mapper.slotPreviewsForTemplate(archetypes, templateId = 42L)
        assertEquals(1, slots.size)
        assertEquals("spread", slots[0].slotKey)
        assertEquals(2, slots[0].variants.size)
        assertTrue(slots[0].variants.any { it.variantKey == "butter" })
        assertTrue(slots[0].variants.any { it.variantKey == "jam" })
        assertEquals(
            listOf("butter", "jam"),
            slots[0].variants.map { it.variantKey },
        )
    }

    @Test
    fun `slotPreviews orders matching template variants before others`() {
        val archetypes = listOf(
            VariabilityArchetype(
                archetypeKey = "toast_breakfast",
                displayName = "Toast breakfast",
                titleAliasesJson = "[]",
                evidenceCount = 2,
                lastSeenTimestamp = null,
                archetypeNotes = null,
                deprecated = false,
                deprecatedReason = null,
                slots = listOf(
                    VariabilitySlot(
                        slotKey = "bread",
                        roleDisplayName = "Bread",
                        nutritionalLeversJson = "[]",
                        isHighVariability = true,
                        confidence = 0.9,
                        rationale = "",
                        variants = listOf(
                            VariabilityVariant(
                                variantKey = "white",
                                variantLabel = "White bread",
                                notesExcerpt = "",
                                evidence = listOf(VariabilityEvidence(loggedAt = "a", templateId = 7L)),
                                sortOrder = 0,
                            ),
                            VariabilityVariant(
                                variantKey = "sourdough",
                                variantLabel = "Sourdough bread",
                                notesExcerpt = "",
                                evidence = listOf(VariabilityEvidence(loggedAt = "b", templateId = 99L)),
                                sortOrder = 1,
                            ),
                        ),
                        sortOrder = 0,
                    ),
                ),
                sortOrder = 0,
            ),
        )
        val slots = mapper.slotPreviewsForTemplate(archetypes, templateId = 99L)
        assertEquals(listOf("sourdough", "white"), slots[0].variants.map { it.variantKey })
    }

    @Test
    fun `slotPreviews empty when no slot cites template`() {
        val archetypes = listOf(
            VariabilityArchetype(
                archetypeKey = "a",
                displayName = "A",
                titleAliasesJson = "[]",
                evidenceCount = 1,
                lastSeenTimestamp = null,
                archetypeNotes = null,
                deprecated = false,
                deprecatedReason = null,
                slots = listOf(
                    VariabilitySlot(
                        slotKey = "s",
                        roleDisplayName = "S",
                        nutritionalLeversJson = "[]",
                        isHighVariability = false,
                        confidence = 0.0,
                        rationale = "",
                        variants = listOf(
                            VariabilityVariant(
                                variantKey = "v",
                                variantLabel = "V",
                                notesExcerpt = "",
                                evidence = listOf(VariabilityEvidence(loggedAt = "z", templateId = 1L)),
                                sortOrder = 0,
                            ),
                        ),
                        sortOrder = 0,
                    ),
                ),
                sortOrder = 0,
            ),
        )
        assertEquals(emptyList<String>(), mapper.toPreviewLines(archetypes, templateId = 999L))
        assertTrue(mapper.slotPreviewsForTemplate(archetypes, templateId = 999L).isEmpty())
    }
}
