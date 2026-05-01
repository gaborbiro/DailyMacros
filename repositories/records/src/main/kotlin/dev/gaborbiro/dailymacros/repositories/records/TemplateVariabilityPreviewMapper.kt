package dev.gaborbiro.dailymacros.repositories.records

import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityArchetype

/**
 * Maps persisted variability archetypes into human-readable lines for a template id
 * (variants whose evidence lists that [templateId]).
 */
class TemplateVariabilityPreviewMapper {

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
