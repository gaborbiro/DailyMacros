package dev.gaborbiro.dailymacros.repositories.records

import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilitySlotPreview
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityVariantPreview
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityArchetype

/**
 * Maps persisted variability archetypes into preview rows for a template id
 * (variants whose evidence lists that [templateId]).
 */
class TemplateVariabilityPreviewMapper {

    /**
     * Slots where at least one variant cites [templateId] in evidence; each row lists **all**
     * variants in that slot (full recombination). Variants that cite this template are listed first
     * so the preview dialog defaults to the starting template’s variant.
     */
    fun slotPreviewsForTemplate(
        archetypes: List<VariabilityArchetype>,
        templateId: Long,
    ): List<TemplateVariabilitySlotPreview> {
        val rows = mutableListOf<TemplateVariabilitySlotPreview>()
        archetypes
            .sortedBy { it.sortOrder }
            .forEach { archetype ->
                archetype.slots
                    .sortedBy { it.sortOrder }
                    .forEach { slot ->
                        val slotTouchesTemplate = slot.variants.any { v ->
                            v.evidence.any { it.templateId == templateId }
                        }
                        if (!slotTouchesTemplate) return@forEach
                        val sortedDistinct = slot.variants
                            .sortedBy { it.sortOrder }
                            .distinctBy { it.variantKey }
                        val matchingThisTemplate = sortedDistinct.filter { v ->
                            v.evidence.any { it.templateId == templateId }
                        }
                        val rest = sortedDistinct.filterNot { v ->
                            v.evidence.any { it.templateId == templateId }
                        }
                        // Put variants that cite this template first so the dropdown defaults to the
                        // current meal’s variant while still listing every option in the slot.
                        val orderedForUi = matchingThisTemplate + rest
                        rows.add(
                            TemplateVariabilitySlotPreview(
                                archetypeKey = archetype.archetypeKey,
                                archetypeDisplayName = archetype.displayName,
                                slotKey = slot.slotKey,
                                roleDisplayName = slot.roleDisplayName,
                                variants = orderedForUi.map { v ->
                                    TemplateVariabilityVariantPreview(
                                        variantKey = v.variantKey,
                                        variantLabel = v.variantLabel.ifBlank { v.variantKey },
                                    )
                                },
                            ),
                        )
                    }
            }
        return rows
    }

    fun toPreviewLines(archetypes: List<VariabilityArchetype>, templateId: Long): List<String> {
        val lines = mutableListOf<String>()
        archetypes
            .sortedBy { it.sortOrder }
            .forEach { archetype ->
                val archetypeHeader = "Archetype: ${archetype.displayName} (${archetype.archetypeKey})"
                var wroteHeader = false
                archetype.slots
                    .sortedBy { it.sortOrder }
                    .forEach { slot ->
                        val matchingVariants = slot.variants
                            .filter { v -> v.evidence.any { it.templateId == templateId } }
                            .sortedBy { it.sortOrder }
                            .distinctBy { it.variantKey }
                        if (matchingVariants.isEmpty()) return@forEach
                        if (!wroteHeader) {
                            lines.add(archetypeHeader)
                            wroteHeader = true
                        }
                        lines.add("  Slot: ${slot.roleDisplayName} (${slot.slotKey})")
                        matchingVariants.forEach { v ->
                            val label = v.variantLabel.ifBlank { v.variantKey }
                            lines.add("    • $label (${v.variantKey})")
                        }
                    }
            }
        return lines
    }

    /**
     * Stable key for a multi-slot variant choice: `slotKey=variantKey` segments sorted by slot key.
     */
    fun combinationKey(
        slotPreviews: List<TemplateVariabilitySlotPreview>,
        slotKeyToVariantKey: Map<String, String>,
    ): String =
        slotPreviews
            .map { it.slotKey }
            .sorted()
            .joinToString("|") { sk -> "$sk=${slotKeyToVariantKey[sk]}" }

    /**
     * Combination keys already present in the profile for this archetype (one key per template id
     * that participates in all [slotPreviews] slots).
     */
    fun existingCombinationKeysForArchetype(
        archetypes: List<VariabilityArchetype>,
        archetypeKey: String,
        slotPreviews: List<TemplateVariabilitySlotPreview>,
    ): Set<String> {
        val archetype = archetypes.find { it.archetypeKey == archetypeKey } ?: return emptySet()
        val slotKeys = slotPreviews.map { it.slotKey }.toSet()
        val slotsInArchetype = archetype.slots.filter { it.slotKey in slotKeys }
        if (slotsInArchetype.size != slotKeys.size) return emptySet()

        val templateIds = mutableSetOf<Long>()
        slotsInArchetype.forEach { slot ->
            slot.variants.forEach { v ->
                v.evidence.forEach { e -> e.templateId?.let { templateIds.add(it) } }
            }
        }

        val keys = mutableSetOf<String>()
        for (tid in templateIds) {
            val parts = mutableListOf<String>()
            var ok = true
            for (sk in slotKeys.sorted()) {
                val slot = slotsInArchetype.find { it.slotKey == sk } ?: run {
                    ok = false
                    break
                }
                val variant = slot.variants.find { v ->
                    v.evidence.any { it.templateId == tid }
                }
                if (variant == null) {
                    ok = false
                    break
                }
                parts.add("$sk=${variant.variantKey}")
            }
            if (ok) keys.add(parts.sorted().joinToString("|"))
        }
        return keys
    }

    /**
     * Returns a template id whose per-slot variant choices match [combinationKey] for [archetypeKey],
     * or null if none.
     */
    fun templateIdForCombinationInArchetype(
        archetypes: List<VariabilityArchetype>,
        archetypeKey: String,
        slotPreviews: List<TemplateVariabilitySlotPreview>,
        combinationKey: String,
    ): Long? {
        val archetype = archetypes.find { it.archetypeKey == archetypeKey } ?: return null
        val slotKeys = slotPreviews.map { it.slotKey }.toSet()
        val slotsInArchetype = archetype.slots.filter { it.slotKey in slotKeys }
        if (slotsInArchetype.size != slotKeys.size) return null

        val templateIds = mutableSetOf<Long>()
        slotsInArchetype.forEach { slot ->
            slot.variants.forEach { v ->
                v.evidence.forEach { e -> e.templateId?.let { templateIds.add(it) } }
            }
        }
        for (tid in templateIds) {
            val key = combinationKeyForTemplateInArchetype(archetypes, archetypeKey, slotPreviews, tid)
            if (key == combinationKey) return tid
        }
        return null
    }

    /**
     * Combination key for [templateId] if it appears in every slot of [slotPreviews] under [archetypeKey].
     */
    fun combinationKeyForTemplateInArchetype(
        archetypes: List<VariabilityArchetype>,
        archetypeKey: String,
        slotPreviews: List<TemplateVariabilitySlotPreview>,
        templateId: Long,
    ): String? {
        val archetype = archetypes.find { it.archetypeKey == archetypeKey } ?: return null
        val parts = mutableListOf<String>()
        for (sp in slotPreviews.sortedBy { it.slotKey }) {
            val slot = archetype.slots.find { it.slotKey == sp.slotKey } ?: return null
            val variant = slot.variants.find { v ->
                v.evidence.any { it.templateId == templateId }
            } ?: return null
            parts.add("${sp.slotKey}=${variant.variantKey}")
        }
        return parts.sorted().joinToString("|")
    }
}
