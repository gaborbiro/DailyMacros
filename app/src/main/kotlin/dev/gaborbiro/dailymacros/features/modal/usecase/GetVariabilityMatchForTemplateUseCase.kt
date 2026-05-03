package dev.gaborbiro.dailymacros.features.modal.usecase

import dev.gaborbiro.dailymacros.repositories.records.TemplateVariabilityPreviewMapper
import dev.gaborbiro.dailymacros.repositories.records.VariabilityProfileMapper
import dev.gaborbiro.dailymacros.repositories.records.domain.VariabilityRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.TemplateVariabilityPreviewContent

internal data class TemplateVariabilityMatch(
    val preview: TemplateVariabilityPreviewContent,
    /** Latest merged profile JSON when [preview.slots] is non-empty; null otherwise. */
    val profileJson: String?,
    val minedAtEpochMs: Long = 0L,
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
            ?: return TemplateVariabilityMatch(
                preview = TemplateVariabilityPreviewContent(
                    bannerText = "No variability profile is loaded yet. Run meal variability mining from Settings to see variants here.",
                    slots = emptyList(),
                    archetypePickerLabel = "",
                ),
                profileJson = null,
                minedAtEpochMs = 0L,
            )
        val profile = profileMapper.parseProfileJson(
            profileJson = snapshot.profileJson,
            minedAtEpochMs = snapshot.minedAtEpochMs,
        )
        val slots = previewMapper.slotPreviewsForTemplate(profile.archetypes, templateId)
        val archetypeLabel = slots.firstOrNull()?.archetypeDisplayName.orEmpty()
        return if (slots.isEmpty()) {
            TemplateVariabilityMatch(
                preview = TemplateVariabilityPreviewContent(
                    bannerText = "No slots in the profile cite this template yet. After the next mine, variants that reference this template id will appear here.",
                    slots = emptyList(),
                    archetypePickerLabel = "",
                ),
                profileJson = null,
                minedAtEpochMs = snapshot.minedAtEpochMs,
            )
        } else {
            TemplateVariabilityMatch(
                preview = TemplateVariabilityPreviewContent(
                    bannerText = "Pick a variant per slot (demo — not saved yet).",
                    slots = slots,
                    archetypePickerLabel = archetypeLabel,
                ),
                profileJson = snapshot.profileJson,
                minedAtEpochMs = snapshot.minedAtEpochMs,
            )
        }
    }
}
