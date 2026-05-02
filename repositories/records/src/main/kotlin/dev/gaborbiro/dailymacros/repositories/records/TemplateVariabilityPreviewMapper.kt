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
                                role = slot.role,
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
                        lines.add("  Slot: ${slot.role} (${slot.slotKey})")
                        matchingVariants.forEach { v ->
                            val label = v.variantLabel.ifBlank { v.variantKey }
                            lines.add("    • $label (${v.variantKey})")
                        }
                    }
            }
        return lines
    }
}
