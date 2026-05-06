package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.features.modal.model.VariabilityArchetypePickerEntry
import dev.gaborbiro.dailymacros.repositories.records.TemplateVariabilityPreviewMapper
import dev.gaborbiro.dailymacros.repositories.records.VariabilityProfileMapper
import dev.gaborbiro.dailymacros.repositories.records.domain.VariabilityRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityPreviewContent
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilitySlotPreview
import dev.gaborbiro.dailymacros.repositories.records.domain.model.variability.VariabilityArchetype

/** Thrown when [VariabilityRepository.getLatestProfile] is null (no mine has been persisted yet). */
internal class NoVariabilityProfileLoadedException : Exception("No meal variability profile is loaded yet.")

internal data class TemplateVariabilityMatch(
    val preview: TemplateVariabilityPreviewContent,
    /** Parsed archetypes from the latest snapshot (empty when no profile / no parse). */
    val variabilityArchetypes: List<VariabilityArchetype>,
    /** One row per archetype that cites this template (separate links in record details). */
    val archetypePickerEntries: List<VariabilityArchetypePickerEntry>,
)

/**
 * Loads mined variability for [templateId] (evidence match) for record details / picker flows.
 */
internal class GetVariabilityMatchForTemplateUseCase(
    private val variabilityRepository: VariabilityRepository,
    private val profileMapper: VariabilityProfileMapper,
    private val previewMapper: TemplateVariabilityPreviewMapper = TemplateVariabilityPreviewMapper(),
) {

    suspend fun execute(templateId: Long): TemplateVariabilityMatch {
        val snapshot = variabilityRepository.getLatestProfile()
            ?: throw NoVariabilityProfileLoadedException()
        val profile = profileMapper.parseProfileJson(
            profileJson = snapshot.profileJson,
            minedAtEpochMs = snapshot.minedAtEpochMs,
            templatesIngestWatermarkEpochMs = snapshot.templatesIngestWatermarkEpochMs,
        )
        val slots = previewMapper.slotPreviewsForTemplate(profile.archetypes, templateId)
        val entries = buildArchetypePickerEntries(profile.archetypes, slots)
        val archetypeLabel = entries.firstOrNull()?.linkTitle
            ?: slots.firstOrNull()?.archetypeDisplayName.orEmpty()
        return if (slots.isEmpty()) {
            TemplateVariabilityMatch(
                preview = TemplateVariabilityPreviewContent(
                    bannerText = "No slots in the profile cite this template yet. After the next mine, variants that reference this template id will appear here.",
                    slots = emptyList(),
                    archetypePickerLabel = "",
                ),
                variabilityArchetypes = profile.archetypes,
                archetypePickerEntries = emptyList(),
            )
        } else {
            TemplateVariabilityMatch(
                preview = TemplateVariabilityPreviewContent(
                    bannerText = "Pick a variant per slot (demo — not saved yet).",
                    slots = slots,
                    archetypePickerLabel = archetypeLabel,
                ),
                variabilityArchetypes = profile.archetypes,
                archetypePickerEntries = entries,
            )
        }
    }

    private fun buildArchetypePickerEntries(
        profileArchetypes: List<VariabilityArchetype>,
        slotPreviews: List<TemplateVariabilitySlotPreview>,
    ): List<VariabilityArchetypePickerEntry> {
        if (slotPreviews.isEmpty()) return emptyList()
        val byKey = slotPreviews.groupBy { it.archetypeKey }
        return profileArchetypes
            .sortedBy { it.sortOrder }
            .mapNotNull { arch ->
                val list = byKey[arch.archetypeKey] ?: return@mapNotNull null
                if (list.isEmpty()) return@mapNotNull null
                VariabilityArchetypePickerEntry(
                    archetypeKey = arch.archetypeKey,
                    linkTitle = arch.displayName.ifBlank { list.first().archetypeDisplayName },
                    slots = list.sortedBy { it.slotKey },
                )
            }
    }
}
