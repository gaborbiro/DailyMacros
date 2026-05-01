package dev.gaborbiro.dailymacros.repositories.records

import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityArchetype
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityEvidence
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilitySlot
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityVariant
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
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
                        role = "Spread",
                        nutritionalLeversJson = "[]",
                        isHighVariability = true,
                        confidence = 0.9,
                        rationale = "",
                        variants = listOf(
                            VariabilityVariant(
                                variantKey = "butter",
                                variantLabel = "Butter",
                                macroSource = "",
                                notesExcerpt = "",
                                typicalMacrosJson = "{}",
                                evidence = listOf(VariabilityEvidence(loggedAt = "x", templateId = 42L)),
                                sortOrder = 0,
                            ),
                            VariabilityVariant(
                                variantKey = "jam",
                                variantLabel = "Jam",
                                macroSource = "",
                                notesExcerpt = "",
                                typicalMacrosJson = "{}",
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
    }

    @Test
    fun `empty when no evidence matches template`() {
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
                        role = "S",
                        nutritionalLeversJson = "[]",
                        isHighVariability = false,
                        confidence = 0.0,
                        rationale = "",
                        variants = listOf(
                            VariabilityVariant(
                                variantKey = "v",
                                variantLabel = "V",
                                macroSource = "",
                                notesExcerpt = "",
                                typicalMacrosJson = "{}",
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
    }
}
