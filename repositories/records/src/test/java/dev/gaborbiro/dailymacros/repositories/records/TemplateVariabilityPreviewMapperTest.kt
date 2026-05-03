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

    @Test
    fun `combinationKey is stable sorted by slot key`() {
        val slots = mapper.slotPreviewsForTemplate(
            listOf(
                VariabilityArchetype(
                    archetypeKey = "x",
                    displayName = "X",
                    titleAliasesJson = "[]",
                    evidenceCount = 1,
                    lastSeenTimestamp = null,
                    archetypeNotes = null,
                    deprecated = false,
                    deprecatedReason = null,
                    slots = listOf(
                        VariabilitySlot(
                            slotKey = "b",
                            roleDisplayName = "B",
                            nutritionalLeversJson = "[]",
                            isHighVariability = true,
                            confidence = 1.0,
                            rationale = "",
                            variants = listOf(
                                VariabilityVariant(
                                    variantKey = "b1",
                                    variantLabel = "B1",
                                    notesExcerpt = "",
                                    evidence = listOf(VariabilityEvidence(loggedAt = "t", templateId = 1L)),
                                    sortOrder = 0,
                                ),
                            ),
                            sortOrder = 1,
                        ),
                        VariabilitySlot(
                            slotKey = "a",
                            roleDisplayName = "A",
                            nutritionalLeversJson = "[]",
                            isHighVariability = true,
                            confidence = 1.0,
                            rationale = "",
                            variants = listOf(
                                VariabilityVariant(
                                    variantKey = "a1",
                                    variantLabel = "A1",
                                    notesExcerpt = "",
                                    evidence = listOf(VariabilityEvidence(loggedAt = "t", templateId = 1L)),
                                    sortOrder = 0,
                                ),
                            ),
                            sortOrder = 0,
                        ),
                    ),
                    sortOrder = 0,
                ),
            ),
            templateId = 1L,
        )
        val key = mapper.combinationKey(slots, mapOf("a" to "a1", "b" to "b1"))
        assertEquals("a=a1|b=b1", key)
    }

    @Test
    fun `templateIdForCombinationInArchetype finds template when both slots cite it`() {
        val archetypes = listOf(
            VariabilityArchetype(
                archetypeKey = "meal",
                displayName = "Meal",
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
                        confidence = 1.0,
                        rationale = "",
                        variants = listOf(
                            VariabilityVariant(
                                variantKey = "jam",
                                variantLabel = "Jam",
                                notesExcerpt = "",
                                evidence = listOf(VariabilityEvidence(loggedAt = "t1", templateId = 10L)),
                                sortOrder = 0,
                            ),
                        ),
                        sortOrder = 0,
                    ),
                    VariabilitySlot(
                        slotKey = "bread",
                        roleDisplayName = "Bread",
                        nutritionalLeversJson = "[]",
                        isHighVariability = true,
                        confidence = 1.0,
                        rationale = "",
                        variants = listOf(
                            VariabilityVariant(
                                variantKey = "white",
                                variantLabel = "White",
                                notesExcerpt = "",
                                evidence = listOf(VariabilityEvidence(loggedAt = "t2", templateId = 10L)),
                                sortOrder = 0,
                            ),
                        ),
                        sortOrder = 1,
                    ),
                ),
                sortOrder = 0,
            ),
        )
        val previews = mapper.slotPreviewsForTemplate(archetypes, templateId = 10L)
        val combo = mapper.combinationKeyForTemplateInArchetype(archetypes, "meal", previews, 10L)
        assertEquals("bread=white|spread=jam", combo)
        val tid = mapper.templateIdForCombinationInArchetype(archetypes, "meal", previews, combo!!)
        assertEquals(10L, tid)
    }
}
